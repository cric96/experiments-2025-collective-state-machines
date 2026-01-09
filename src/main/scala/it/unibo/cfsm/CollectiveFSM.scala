package it.unibo.cfsm

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, ScafiAlchemistSupport}
trait CollectiveFSM {
  self: AggregateProgram with ScafiAlchemistSupport =>
  def cfsm[S: Ordering](initial: S)(logic: S => Next[S])(implicit module: HistoryModule): S = {
    import module._ // operations
    share(module.createHistory[S](initial)) { (history, nbrHistory) =>
      val next = align(history.current.state)(logic(_))
      node.put("history", module.render(history))
      foldhood[module.H[S]](history.add(next))(module.max)(nbrHistory())
        .replaceWhenLooping(next)
    }.current.state
  }
}
