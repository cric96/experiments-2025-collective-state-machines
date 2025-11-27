package it.unibo.cfsm

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, ScafiAlchemistSupport}
trait CollectiveFSM {
  self: AggregateProgram with ScafiAlchemistSupport =>

  def cfsm[S: Ordering](initial: S)(logic: S => Next[S]): S = {
    share(History[S](initial)) { (history, nbrHistory) =>
      val maxHistory = foldhood[History[S]](history)(History.max){nbrHistory()}
      node.put("cfsmHistory", maxHistory.toString)
      val currentState = maxHistory.current.state
      val next = align(currentState) { logic(_) }
      maxHistory.add(next)
    }.current.state
  }
}
