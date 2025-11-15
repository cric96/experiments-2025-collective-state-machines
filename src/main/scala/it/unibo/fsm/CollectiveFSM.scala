package it.unibo.fsm

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.AggregateProgram
trait CollectiveFSM {
  self: AggregateProgram =>

  def fsm[S: Ordering](initial: S)(logic: S => State[S]): S = {
    share(History[S](initial)) { (history, nbrHistory) =>
      val maxHistory = foldhood(history)((x, y) => History.max(x, y)){nbrHistory()}
      val currentState = maxHistory.current.state
      val next = align(currentState) { logic(_) }
      maxHistory.add(next)
    }.current.state
  }
}
