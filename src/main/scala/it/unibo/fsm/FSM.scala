package it.unibo.fsm

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, Builtins}
trait FSM {
  self: AggregateProgram =>

  def fsm[S: Ordering](initial: S)(logic: S => State[S]): S = {
    rep(History[S](initial)) { history =>
      val maxHistory = foldhood(history)((x, y) => History.max(x, y)){nbr(history)}
      val currentState = maxHistory.current.state
      val next = align(currentState) {
        state => logic(state)
      }
      maxHistory.add(next)
    }.current.state
  }
}
