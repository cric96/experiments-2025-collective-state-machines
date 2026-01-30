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
    with FieldUtils
    with PatternFormationLib
    with BlocksWithShare
    with BlockS
    with BlocksWithGC
    with ProcessFix
    with CustomSpawn
    with BaseMovementLib
    with FlockLib
    with ScafiAlchemistSupport
    with GPSMovement
    with CollectiveFSM
    with LeaderBasedLib
    with TeamFormationLib
    with MacroswarmFix {
  import CaseStudy._

  implicit def historyModule = {
    val maxLife = sense[Int]("historyLength")
    BoundedHistory.create(() => alchemistTimestamp.toDouble, maxLife = maxLife)
  }

  private lazy val bound = sense[Double]("side") / 2.0
  private lazy val visionRange = sense[Double]("visionRange")
  private lazy val initialPosition = currentPosition()

  private def bottom: Point3D = Point3D(0, -bound, 0.0)
  private def top: Point3D = Point3D(bound * 2, bound, 0.0)

  override protected def movementLogic(): Point3D = {
    val basePosition = initialPosition
    val result = cfsm[MovementState](Wait()) {
      case Wait() => handlingWaiting(basePosition)
      case Wandering() => handleWandering()
      case Defending() => handlingDefending(basePosition)
      case Solving() => handlingSolving()
    }
    updateStats(result.state)
    result.velocity
  }

  private def handleWandering(): Next[MovementState] = {
    import CaseStudy._
    val leader = S(Double.PositiveInfinity, nbrRange)
    val velocity = repeatedlyWanderingOnCornersAndCenter(leader) / 2.0
    val leaderVelocity = alignWithLeader(leader, velocity)
    val computedVelocity = rep(Point3D.Zero) { velocity =>
      val separationForce = computeSeparationForce(Math.min(visionRange * 0.5, SEPARATION_RADIUS))
      val toLeader = sinkAt(leader)
      val alignmentForce = leaderVelocity.normalize
      val targetVel = (separationForce * SEPARATION_WEIGHT
        + toLeader * COHESION_WEIGHT
        + alignmentForce * ALIGNMENT_WEIGHT).normalize
      smoothVelocity(velocity, targetVel)
    }
    Wandering().updateVelocity(velocity)
    if (baseAttacked) {
      Defending() --> Double.PositiveInfinity
    } else if (anyProblemFound) {
      Solving()
    } else {
      Wandering().updateVelocity(mux(leader)(velocity / 2)(computedVelocity))
    }
  }

  private def handlingWaiting(basePosition: Point3D): Next[MovementState] = {
    if (anyAlarm) {
      Wandering()
    } else if (baseAttacked) {
      Defending() --> Double.PositiveInfinity
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
    import CaseStudy._
    val goalPosition = broadcast(positionOfProblem.isDefined, positionOfProblem)
    val movement = rep(Point3D.Zero) { oldVelocity =>
      val separationForce = computeSeparationForce(SEPARATION_RADIUS_SOLVING)
      goalPosition match {
        case None =>
          val wanderVelocity = explore(bottom, top, maxVelocity = 1.0)
          val targetVel = (separationForce * SEPARATION_WEIGHT + wanderVelocity).normalize
          smoothVelocity(oldVelocity, targetVel)
        case Some(target) =>
          val targetVel = (goto(target) + separationForce * SEPARATION_WEIGHT).normalize
          smoothVelocity(oldVelocity, targetVel)
      }
    }
    if (isSolved) {
      Wait()
    } else {
      Solving().updateVelocity(movement)
    }
  }

  private def updateStats(state: MovementState): Unit = {
    node.put("hue", state.numeric * 5)
    node.put("state", state.numeric)
    MoleculeConstants.updateState(alchemistEnvironment.getNodeByID(mid()), state)
  }

  // External sensors
  private def anyAlarm: Boolean =
    ExternalSensors.anyAlarm(alchemistEnvironment, mid())

  private def anyProblemFound: Boolean =
    ExternalSensors.anyProblemFound(alchemistEnvironment, mid())

  private def positionOfProblem: Option[Point3D] =
    ExternalSensors.positionOfProblem(alchemistEnvironment, mid())

  private def baseAttacked: Boolean =
    ExternalSensors.baseAttacked(alchemistEnvironment, mid())

  private def baseDefended: Boolean =
    ExternalSensors.baseDefended(alchemistEnvironment, mid())

  private def isSolved: Boolean =
    ExternalSensors.isSolved(alchemistEnvironment, mid())

  private def computeSeparationForce(separationRadius: Double = SEPARATION_RADIUS): Point3D = {
    import CaseStudy._
    foldhood(Point3D.Zero)(_ + _) {
      val dist = Math.max(nbrRange(), MIN_DISTANCE)
      val vec = nbrVector()

      mux(dist < separationRadius && mid() != nbr(mid())) {
        val strength = (separationRadius / dist) * (separationRadius / dist)
        val awayDir = mux(vec.module > VECTOR_MODULE_THRESHOLD)(-vec.normalize)(brownian(1.0).normalize)
        awayDir * strength
      } {
        Point3D.Zero
      }
    }
  }

  def repeatedlyWanderingOnCornersAndCenter(leader: Boolean): Point3D = {
    val bottomLeft = Point3D(0, -bound, 0.0)
    val topRight = Point3D(bound * 2, bound, 0.0)
    explore(bottomLeft, topRight, maxVelocity = 1.0)
  }

  /** Smoothly blends previous velocity with target velocity */
  private def smoothVelocity(previousVelocity: Point3D, targetVelocity: Point3D): Point3D =
    previousVelocity * (1 - CaseStudy.SMOOTHING_FACTOR) + targetVelocity * CaseStudy.SMOOTHING_FACTOR
}

object CaseStudy {
  // Separation behavior constants
  val SEPARATION_RADIUS: Double = 5
  val SEPARATION_RADIUS_SOLVING: Double = 2.0
  val MIN_DISTANCE: Double = 0.5
  val VECTOR_MODULE_THRESHOLD: Double = 0.01
  // Force weighting constants
  val SEPARATION_WEIGHT: Double = 1.5
  val COHESION_WEIGHT: Double = 0.5
  val ALIGNMENT_WEIGHT = 0.5

  // Smoothing constants
  val SMOOTHING_FACTOR: Double = 0.3

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
