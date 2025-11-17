package it.unibo.fsm
import scala.math.Ordering.Implicits.seqOrdering
import scala.math.Ordering.Implicits._
case class History[S: Ordering](initial: S, val states: List[State[S]] = List()) {
  def add(state: State[S]): History[S] = {
    states.headOption match {
      case Some(oldHead) if oldHead.same(state) && oldHead < state => this.copy(states = state :: states.tail)
      case Some(oldHead) if oldHead.same(state) => this
      case Some(_) | None => this.copy(states = state :: states)
    }
  }

  def current: State[S] =
    states.headOption.getOrElse(State(initial, Double.MinValue))

  override def toString: String = {
    s"H ~ ${(states.map(s => s"$s)").appended(s"$initial")).reverse.mkString(" -> ")}"
  }
}


object History {
  implicit def historyOrdering[S: Ordering]: Ordering[History[S]] = {
    Ordering.by[History[S], List[State[S]]](_.states.reverse)
  }

  def max[S: Ordering](h1: History[S], h2: History[S]): History[S] = {
    if (historyOrdering[S].gt(h1, h2)) h1 else h2
  }
}