package it.unibo.fsm

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, ScafiAlchemistSupport}
trait CollectiveFSM {
  self: AggregateProgram with ScafiAlchemistSupport =>

  def fsm[S: Ordering](initial: S)(logic: S => State[S]): S = {
    share(History[S](initial)) { (history, nbrHistory) =>
      val maxHistory = foldhood[History[S]](history)(History.max){nbrHistory()}
      node.put("fsmHistory", maxHistory.toString)
      val currentState = maxHistory.current.state
      val next = align(currentState) { logic(_) }
      maxHistory.add(next)
    }.current.state
  }
}
