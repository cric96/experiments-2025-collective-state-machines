package it.unibo.fsm
import scala.math.Ordering.Implicits.seqOrdering

case class History[S: Ordering](initial: S, val states: List[State[S]] = List()) {
  def add(state: State[S]): History[S] = {
    statespò.headOption match {
      case Some(oldHead) if oldHead.same(state) => this.copy(states = state :: states.tail)
      case Some(_) | None => this.copy(states = state :: states)
    }
  }

  def current: State[S] =
    states.headOption.getOrElse(State(initial, Double.MinValue))

  override def toString: String = {
    s"History(${(initial :: states.map(_.state)).reverse.mkString(" -> ")})"
  }
}


object History {
  implicit def historyOrdering[S: Ordering]: Ordering[History[S]] = {
    Ordering.by[History[S], List[State[S]]](_.states)
  }

  def max[S: Ordering](histories: List[History[S]]): History[S] = {
    histories.max
  }

  def max[S: Ordering](h1: History[S], h2: History[S]): History[S] = {
    if (historyOrdering[S].gt(h1, h2)) h1 else h2
  }
}