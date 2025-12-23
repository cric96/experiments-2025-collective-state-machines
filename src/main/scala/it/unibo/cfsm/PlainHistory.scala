package it.unibo.cfsm
import scala.math.Ordering.Implicits.seqOrdering

protected case class PlainHistory[S: Ordering](transitions: List[Next[S]] = List())

object PlainHistory extends HistoryModule {
  type H[S] = PlainHistory[S]

  def createHistory[S: Ordering](initial: S): H[S] = PlainHistory(List(first(initial)))

  def first[S: Ordering](state: S): Next[S] = Next(state, Double.PositiveInfinity)

  def add[S: Ordering](history: PlainHistory[S], next: Next[S]): PlainHistory[S] = {
    history.transitions match {
      case last :: previous :: tail
        if Next.same(last, previous) => //&& Next.same(last, next) =>
        history.copy(transitions = next :: previous :: tail)
      case _ =>
        history.copy(transitions = next :: history.transitions)
    }
  }

  def current[S: Ordering](history: PlainHistory[S]): Next[S] =
    history.transitions.head

  def replaceWhenLooping[S: Ordering](history: PlainHistory[S], next: Next[S]): PlainHistory[S] = {
    history.transitions match {
      case last :: previous :: tail
        if Next.same(last, previous) && Next.same(next, last) =>
        history.copy(transitions = next :: previous :: tail)
      case _ => history
    }
  }

  def max[S: Ordering](h1: PlainHistory[S], h2: PlainHistory[S]): PlainHistory[S] = {
    if (historyOrdering[S].gt(h1, h2)) h1 else h2
  }

  def render[S: Ordering](history: PlainHistory[S]): String = {
    s"H ~ ${(history.transitions.map(s => s"$s)")).reverse.mkString(" -> ")}"
  }

  implicit def historyOrdering[S: Ordering]: Ordering[PlainHistory[S]] = {
    Ordering.by[PlainHistory[S], List[Next[S]]](_.transitions.reverse)
  }
}