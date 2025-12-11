package it.unibo.cfsm
import scala.math.Ordering.Implicits.seqOrdering
case class History[S: Ordering](initial: S, val states: List[Next[S]] = List()) {
  def add(next: Next[S]): History[S] = {
    states match {
      case last :: previous :: tail
          if Next.same(last, previous) && Next.same(last, next) =>
        this.copy(states = next :: previous :: tail)
      case _ =>
        this.copy(states = next :: states)
    }
  }

  def current: Next[S] =
    states.headOption.getOrElse(Next(initial, Double.MinValue))

  def replaceWhenLooping(next: Next[S]): History[S] = {
    states match {
      case last :: previous :: tail
          if Next.same(last, previous) && Next.same(next, last) =>
        this.copy(states = next :: previous :: tail)
      case _ => this
    }
  }

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