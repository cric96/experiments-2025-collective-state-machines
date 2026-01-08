package it.unibo.program

import it.unibo.cfsm.Next._
import it.unibo.cfsm.{BoundedHistory, CollectiveFSM, Next}
import it.unibo.scafi.macroswarm.MacroSwarmAlchemistSupport._
import it.unibo.scafi.macroswarm.MacroSwarmAlchemistSupport.incarnation._
import it.unibo.scafi.space.Point3D
import it.unibo.scafi.space.pimp._
class CaseStudy
  extends MacroSwarmProgram with StandardSensors with TimeUtils
    with PatternFormationLib with BlocksWithShare with BlocksWithGC
    with ProcessFix with CustomSpawn with BaseMovementLib
    with ScafiAlchemistSupport with GPSMovement with CollectiveFSM {
  import CaseStudy._
  implicit def historyModule = BoundedHistory.create(() => alchemistTimestamp.toDouble)

  private lazy val bound = sense[Double]("side") / 2.0
  private lazy val initialPosition = currentPosition()
  private def bottom: Point3D = Point3D(0, -bound, 0.0)
  private def top: Point3D = Point3D(bound * 2, bound, 0.0)

  override protected def movementLogic(): Point3D = {
    val basePosition = initialPosition
    val result = cfsm[MovementState](Exploring()) {
      case Exploring() => handleExploring()
      case Waiting() => Waiting().updateVelocity(goto(basePosition, maxVelocity = 0.1))
    }
    updateVisualization(result.state)
    result.velocity
  }

  private def handleExploring(): Next[MovementState] = {
    val velocity = explore(bottom, top, maxVelocity = 1.0)
    if(alchemistTimestamp.toDouble > 1000) {
      Waiting().updateVelocity(velocity) --> 1.0
    } else {
      Exploring().updateVelocity(velocity)
    }
  }

  private def updateVisualization(state: MovementState): Unit = {
    val hue = state match {
      case Exploring() => 0
      case Waiting() => 1
    }
    node.put("hue", hue)
  }

  override def goto(destination: Point3D, maxVelocity: Double = 1): Point3D = {
    val distance = currentPosition().distance(destination)
    val direction = (destination) - (currentPosition())
    val velocity = direction * (1 / distance)
    val scaledVelocity = math.min(distance, maxVelocity)
    if(distance < 1e-3) Point3D.Zero else
    velocity * scaledVelocity
  }
}

object CaseStudy {
  sealed trait MovementState extends EqualsUsingOrdering[MovementState] {
    var velocity: Point3D = Point3D.Zero
    def updateVelocity(v: Point3D): MovementState = {
      velocity = v
      this
    }
  }
  case class Exploring() extends MovementState
  case class Waiting() extends MovementState
  implicit def orderingState[E <: MovementState]: Ordering[E] = Ordering.by {
    case Exploring() => 0
    case Waiting() => 1
  }
}