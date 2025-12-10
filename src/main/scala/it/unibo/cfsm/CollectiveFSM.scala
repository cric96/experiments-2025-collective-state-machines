package it.unibo.cfsm

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, ScafiAlchemistSupport}
trait CollectiveFSM {
  self: AggregateProgram with ScafiAlchemistSupport =>

  def cfsm[S: Ordering](initial: S)(logic: S => Next[S]): S = {
    share(History[S](initial)) { (history, nbrHistory) =>
      val next = align(history.current.state) { logic(_) }
      foldhood[History[S]](history.add(next))(History.max){nbrHistory()}
    }.current.state
  }
}
