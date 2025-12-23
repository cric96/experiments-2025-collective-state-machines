package it.unibo.program

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, ScafiAlchemistSupport, StandardSensors}
import it.unibo.cfsm.Next.AnyToNext
import it.unibo.cfsm.{CollectiveFSM, PlainHistory}

class PriorityBasedOnLeader extends AggregateProgram
  with StandardSensors
  with ScafiAlchemistSupport
  with CollectiveFSM {

  implicit def historyModule = PlainHistory


  override def main(): Any = {
    val state = cfsm(-1) {
      _ => mid() --> mid()
    }
    node.put("state", state)
  }
}
