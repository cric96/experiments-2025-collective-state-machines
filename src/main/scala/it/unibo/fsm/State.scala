package it.unibo.fsm

import scala.language.implicitConversions

case class State[+S: Ordering](state: S, priority: Double) {
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

  def same[S](s1: State[S], s2: State[S])(implicit ord: Ordering[S]): Boolean = {
    ord.equiv(s1.state, s2.state)
  }

  abstract class EqualsUsingOrdering[S: Ordering]() {
    override def equals(obj: Any): Boolean = {
      obj match {
        case that: S => Ordering[S].equiv(this.asInstanceOf[S], that)
        case _ => false
      }
    }
  }
}
