package it.unibo.cfsm
import scala.math.Ordering.Implicits.seqOrdering
import scala.math.Ordering.Implicits._
case class History[S: Ordering](initial: S, val states: List[Next[S]] = List()) {
  def add(next: Next[S]): History[S] = {
    states.headOption match {
      case Some(oldHead) if Next.same(oldHead, next) => this.copy(states = next :: states.tail)
      // case Some(oldHead) if State.same(oldHead, state) => this
      case Some(_) | None => this.copy(states = next :: states)
    }
  }

  def current: Next[S] =
    states.headOption.getOrElse(Next(initial, Double.MinValue))

  override def toString: String = {
    s"H ~ ${(states.map(s => s"$s)").appended(s"$initial")).reverse.mkString(" -> ")}"
  }
}


object History {
  implicit def historyOrdering[S: Ordering]: Ordering[History[S]] = {
    Ordering.by[History[S], List[Next[S]]](_.states.reverse)
  }

  def max[S: Ordering](h1: History[S], h2: History[S]): History[S] = {
    if (historyOrdering[S].gt(h1, h2)) h1 else h2
  }
}