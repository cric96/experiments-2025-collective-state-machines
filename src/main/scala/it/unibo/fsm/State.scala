package it.unibo.fsm

case class State[S: Ordering](state: S, priority: Double) {
  def same(other: State[S]): Boolean = {
    other.state == state
  }

  override def toString: String =
    s"($state, ${renderNumber}"

  private def renderNumber: String = if(priority.isNegInfinity) { "_" } else { priority.toString }
}
object State {
  // ordering considering the priority first, then the state
  implicit def stateOrdering[S: Ordering]: Ordering[State[S]] = {
    Ordering.by[State[S], (Double, S)](s => (s.priority, s.state))
  }
  // auto conversion from S to State[S] with minimum priority
  implicit def fromState[S: Ordering](s: S): State[S] = {
    State[S](s, Double.NegativeInfinity)
  }

  implicit class AnyToState[S: Ordering](val s: S) {
    def -->(priority: Double): State[S] = {
      State[S](s, priority)
    }
  }
}
