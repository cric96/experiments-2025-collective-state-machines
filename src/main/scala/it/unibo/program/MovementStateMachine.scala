package it.unibo.program

import it.unibo.fsm.{CollectiveFSM, State}
import it.unibo.fsm.State._
import it.unibo.scafi.macroswarm.MacroSwarmAlchemistSupport._
import it.unibo.scafi.macroswarm.MacroSwarmAlchemistSupport.incarnation._
import it.unibo.scafi.space.Point3D

class MovementStateMachine
  extends MacroSwarmProgram with StandardSensors with TimeUtils
    with PatternFormationLib with BlocksWithShare with BlocksWithGC
    with ProcessFix with CustomSpawn with BaseMovementLib
    with ScafiAlchemistSupport with GPSMovement with CollectiveFSM {
  import MovementStateMachine._

  private val bound = 500
  private def bottom: Point3D = Point3D(-bound, -bound, 0.0)
  private def top: Point3D = Point3D(bound, bound, 0.0)

  override protected def movementLogic(): Point3D = {
    val result = fsm[MovementState](Exploring()) {
      case Exploring() => handleExploring()
      case FoundProblem(leader) => handleFormingCircle(leader)
      case OnShape(leader) => handleOnShape(leader)
    }
    updateVisualization(result.state)
    result.velocity
  }

  private def handleExploring(): State[MovementState] = {
    val velocity = explore(bottom, top, maxVelocity = 1.0)
    if (foundProblem) {
      FoundProblem(mid()) --> 1.0
    } else {
      Exploring().updateVelocity(velocity)
    }
  }

  private def handleFormingCircle(leader: Int): State[MovementState] = {
    val isLeader = leader == mid()
    val velocity = centeredCircle(isLeader, radius = 100, 1)
    if (isCircleFormed(isLeader, targetDistance = 100, confidence = 1)) {
      OnShape(leader)()
    } else {
      FoundProblem(leader).updateVelocity(velocity)
    }
  }

  private def handleOnShape(leader: Int): State[MovementState] = {
    val exploreAction = explore(bottom, top, maxVelocity = 0.5)
    val isLeader = leader == mid()
    val velocity = centeredCircle(isLeader, radius = 100, 1, leaderVelocity = exploreAction)
    if (resolveProblem) {
      Exploring().updateVelocity(velocity) --> 1.0
    } else {
      OnShape(leader).updateVelocity(velocity)
    }
  }
  private def updateVisualization(state: MovementState): Unit = {
    val hue = state match {
      case Exploring() => 0
      case FoundProblem(_) => 1
      case OnShape(_) => 2
    }
    node.put("hue", hue)
  }

  private def foundProblem: Boolean = rep(0)(_ + 1) > 1000 && mid() == 1
  private def resolveProblem: Boolean = rep(0)(_ + 1) > 5000
}

object MovementStateMachine {
  sealed trait MovementState extends EqualsUsingOrdering[MovementState] {
    var velocity: Point3D = Point3D.Zero
    def updateVelocity(v: Point3D): MovementState = {
      velocity = v
      this
    }
  }
  case class Exploring() extends MovementState
  case class FoundProblem(leader: Int) extends MovementState
  case class OnShape(leader: Int)() extends MovementState

  implicit def orderingState[E <: MovementState]: Ordering[E] = Ordering.by {
    case OnShape(leader: Int) => (2, leader)
    case FoundProblem(leader) => (1, leader)
    case Exploring() => (0, 0)
  }
}