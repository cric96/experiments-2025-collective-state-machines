package it.unibo.program

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, ScafiAlchemistSupport, StandardSensors}
import it.unibo.fsm.FSM
import it.unibo.fsm.State.AnyToState

class ScafiProgram extends AggregateProgram
    with StandardSensors
    with ScafiAlchemistSupport
    with FSM {
  val waitState = 0
  val workState = 1

  override def main(): Any = {
    val state = fsm(waitState) {
      case `waitState` =>
        val counter = rep(0)(_ + 1)
        if (counter > 100 && mid() == 0) {
          workState --> 1.0
        } else {
          waitState
        }
      case `workState` =>
        val counter = rep(0)(_ + 1)
        if (counter > 50) {
          waitState --> 1.0
        } else {
          workState
        }
    }

  }
}
