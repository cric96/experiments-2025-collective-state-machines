package it.unibo.cfsm

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, ScafiAlchemistSupport}

/** A trait providing collective finite state machine (CFSM) capabilities for aggregate programs. */
trait CollectiveFSM {
  self: AggregateProgram with ScafiAlchemistSupport =>

  /** A collective finite state machine (CFSM) implementation that manages state transitions and history across a
    * distributed system.
    * @param initial
    *   the initial state of the CFSM
    * @param logic
    *   a function defining the state transition logic
    * @param module
    *   the history module to use for managing state history
    * @tparam S
    * @return
    *   the current state of the CFSM
    */
  def cfsm[S: Ordering](initial: S)(logic: S => Next[S])(implicit module: HistoryModule): S = {
    import module._ // operations
    share(module.createHistory[S](initial)) { (history, nbrHistory) =>
      val next = align(history.current.state)(logic(_))
      node.put("historySize", module.size(history))
      foldhood[module.H[S]](history.add(next))(module.max)(nbrHistory())
        .replaceWhenLooping(next)
    }.current.state
  }
}
