package it.unibo.program

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, ScafiAlchemistSupport, StandardSensors}
import it.unibo.cfsm.Next.AnyToNext
import it.unibo.cfsm.{CollectiveFSM, PlainHistory}

class SimpleStateMachine extends AggregateProgram
    with StandardSensors
    with ScafiAlchemistSupport
    with CollectiveFSM {
  implicit def historyModule = PlainHistory

  val waitState = 0
  val workState = 1

  override def main(): Any = {
    val state = cfsm(waitState) {
      case `waitState` =>
        rep(0)(_ + 1) match {
          case value if value > 100 && mid() == 0 => workState --> 1.0
          case _ => waitState
        }
      case `workState` =>
        rep(0)(_ + 1) match {
          case value if value > 200 => waitState --> 1.0
          case _ => workState
        }
    }
    node.put("state", state)
  }
}
