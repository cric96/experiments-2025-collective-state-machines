package it.unibo.program

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, ScafiAlchemistSupport, StandardSensors}
import it.unibo.cfsm.CollectiveFSM
import it.unibo.cfsm.Next.AnyToNext

class BranchingHistoryMachine extends AggregateProgram
    with StandardSensors
    with ScafiAlchemistSupport
    with CollectiveFSM {

  val waitState = 0
  val middleState = 1
  val finishState = 2

  override def main(): Any = {
    val state = cfsm(waitState) {
      case `waitState` =>
        if(mid() == 0) {
          finishState --> 1.0
        } else {
          middleState
        }
      case `middleState` => waitState
      case `finishState` => finishState
    }
    node.put("state", state)
  }
}
