package it.unibo.cfsm

import scala.language.implicitConversions

case class Next[+S: Ordering](state: S, priority: Double) {
  override def toString: String =
    s"($state, $renderNumber"

  private def renderNumber: String =
    if (priority.isNegInfinity) { "_" }
    else if (priority.isPosInfinity) { "+∞" }
    else { priority.toString }
}
object Next {
  // ordering considering the priority first, then the state
  implicit def nextOrdering[S: Ordering]: Ordering[Next[S]] =
    Ordering.by[Next[S], (Double, S)](s => (s.priority, s.state))
  // auto conversion from S to State[S] with minimum priority
  implicit def fromState[S: Ordering](s: S): Next[S] =
    Next[S](s, Double.NegativeInfinity)

  implicit class AnyToNext[S: Ordering](val s: S) {
    def -->(priority: Double): Next[S] =
      Next[S](s, priority)
  }

  def same[S](s1: Next[S], s2: Next[S])(implicit ord: Ordering[S]): Boolean =
    ord.equiv(s1.state, s2.state)

  abstract class EqualsUsingOrdering[S: Ordering]() {
    override def equals(obj: Any): Boolean = {
      obj match {
        case that: S => Ordering[S].equiv(this.asInstanceOf[S], that)
        case _ => false
      }
    }
  }
}
