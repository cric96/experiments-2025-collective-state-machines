package it.unibo.fsm

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, Builtins}
trait FSM {
  self: AggregateProgram =>

  def fsm[S: Ordering](initial: S)(logic: S => State[S]): S = {
    share(History[S](initial)) { (history, nbrHistory) =>
      val maxHistory = foldhood(history)((x, y) => History.max(x, y)){nbrHistory()}
      val currentState = maxHistory.current.state
      val next = align(currentState) {
        state => logic(state)
      }
      maxHistory.add(next)
    }.current.state
  }
}
