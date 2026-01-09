package it.unibo.program

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.molecules.MoleculeConstants
import it.unibo.cfsm.Next._
import it.unibo.cfsm.{BoundedHistory, CollectiveFSM, Next}
import it.unibo.macroswarm.MacroswarmFix
import it.unibo.scafi.macroswarm.MacroSwarmAlchemistSupport._
import it.unibo.scafi.macroswarm.MacroSwarmAlchemistSupport.incarnation._
import it.unibo.scafi.space.Point3D
import it.unibo.scafi.space.pimp._

import scala.jdk.CollectionConverters.IteratorHasAsScala
class CaseStudy
    extends MacroSwarmProgram
    with StandardSensors
    with TimeUtils
    with PatternFormationLib
    with BlocksWithShare
    with BlocksWithGC
    with ProcessFix
    with CustomSpawn
    with BaseMovementLib
    with FlockLib
    with ScafiAlchemistSupport
    with GPSMovement
    with CollectiveFSM
    with MacroswarmFix {
  import CaseStudy._
  implicit def historyModule = BoundedHistory.create(() => alchemistTimestamp.toDouble, maxLife = 10000)

  private lazy val bound = sense[Double]("side") / 2.0
  private lazy val visionBaseRange = 10 // inside the square
  private lazy val visionProblemRange = 100.0 * 0.25 // smaller vision
  private lazy val initialPosition = currentPosition()

  private def bottom: Point3D = Point3D(0, -bound, 0.0)
  private def top: Point3D = Point3D(bound * 2, bound, 0.0)

  override protected def movementLogic(): Point3D = {
    val basePosition = initialPosition
    val result = cfsm[MovementState](Wait()) {
      case Wait() => handlingWaiting(basePosition)
      case Wandering() => handleExploring()
      case Defending() => handlingDefending(basePosition)
      case Solving() => handlingSolving()
    }
    updateStats(result.state)
    result.velocity
  }

  private def handleExploring(): Next[MovementState] = {
    val velocity = explore(bottom, top, maxVelocity = 1.0)
    Wandering().updateVelocity(velocity)
    if (baseAttacked) {
      Defending()
    } else if (anyProblemFound) {
      Solving()
    } else {
      Wandering().updateVelocity(velocity)
    }
  }

  private def handlingWaiting(basePosition: Point3D): Next[MovementState] = {
    if (anyAlarm) {
      Wandering()
    } else if (baseAttacked) {
      Defending()
    } else {
      val velocity = goto(basePosition, maxVelocity = 0.1)
      Wait().updateVelocity(velocity)
    }
  }

  private def handlingDefending(basePosition: Point3D): Next[MovementState] = {
    if (baseDefended) {
      Wait()
    } else {
      Defending().updateVelocity(goto(basePosition))
    }
  }

  private def handlingSolving(): Next[MovementState] = {
    val goalPosition = broadcast(positionOfProblem.isDefined, positionOfProblem.getOrElse(currentPosition()))
    val movement = rep(Point3D.Zero)(oldVelocity =>
      (goto(goalPosition) + separation(oldVelocity, OneHopNeighbourhoodWithinRange(2)) * 2).normalize
    )
    if (isSolved) {
      Wait()
    } else {
      Solving().updateVelocity(movement)
    }
  }

  private def updateStats(state: MovementState): Unit = {
    node.put("hue", state.numeric * 5)
    MoleculeConstants.updateState(alchemistEnvironment.getNodeByID(mid()), state)
  }

  // External sensors
  private def anyAlarm: Boolean =
    ExternalSensors.anyAlarm(alchemistEnvironment, mid(), visionBaseRange)

  private def anyProblemFound: Boolean =
    ExternalSensors.anyProblemFound(alchemistEnvironment, mid(), visionProblemRange)

  private def positionOfProblem: Option[Point3D] =
    ExternalSensors.positionOfProblem(alchemistEnvironment, mid(), visionProblemRange)

  private def baseAttacked: Boolean =
    ExternalSensors.baseAttacked(alchemistEnvironment, mid(), visionBaseRange)

  private def baseDefended: Boolean =
    ExternalSensors.baseDefended(alchemistEnvironment, mid(), visionBaseRange)

  private def isSolved: Boolean =
    ExternalSensors.isSolved(alchemistEnvironment, mid(), visionProblemRange)

  private def nodes: Seq[Node[Any]] =
    alchemistEnvironment.getNodes.iterator().asScala.toSeq
}

object CaseStudy {
  sealed trait MovementState extends EqualsUsingOrdering[MovementState] {
    var velocity: Point3D = Point3D.Zero
    def updateVelocity(v: Point3D): MovementState = {
      velocity = v
      this
    }
    def numeric: Int = this match {
      case Wait() => 0
      case Wandering() => 1
      case Defending() => 2
      case Solving() => 3
    }
  }
  def allCases: Map[Class[_ <: MovementState], String] = Map(
    classOf[Wandering] -> "wandering",
    classOf[Wait] -> "wait",
    classOf[Defending] -> "defending",
    classOf[Solving] -> "solving"
  )
  case class Wandering() extends MovementState
  case class Wait() extends MovementState
  case class Defending() extends MovementState
  case class Solving() extends MovementState
  implicit def orderingState[E <: MovementState]: Ordering[E] = Ordering.by {
    _.numeric
  }
}
