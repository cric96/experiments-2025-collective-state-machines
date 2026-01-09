package it.unibo.program

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{
  AggregateProgram,
  ScafiAlchemistSupport,
  StandardSensors
}
import it.unibo.cfsm.Next.AnyToNext
import it.unibo.cfsm.{CollectiveFSM, PlainHistory}

class AtLeastNMachine extends AggregateProgram with StandardSensors with ScafiAlchemistSupport with CollectiveFSM {

  implicit def historyModule = PlainHistory

  case class AgreedState(ids: Set[Int]) {
    def renderState: Int = ids.size
  }

  implicit def orderingAgreedState[S <: AgreedState]: Ordering[S] = Ordering.by { s =>
    (s.ids.size, s.ids.maxOption.getOrElse(0))
  }

  def condition: Boolean = {
    val counter = rep(0)(_ + 1)
    if (counter < 10) { mid() < 3.0 }
    else {
      mid() > 10.0 && mid() < 21.0
    }
  }

  def barrier(n: Int, condition: Boolean)(onBroken: => Unit): Unit = {
    val agreed = cfsm[AgreedState](AgreedState(Set.empty)) {
      case AgreedState(ids) if condition && !ids.contains(mid()) && ids.size < n =>
        AgreedState(ids + mid()) --> 1.0
      case AgreedState(ids) if !condition && ids.contains(mid()) =>
        AgreedState(ids - mid()) --> 2.0 // Higher priority to remove self from the set
      case s => s
    }
    node.put("state", agreed.renderState)
    if (agreed.ids.size >= n) onBroken
  }
  override def main(): Any = {
    val needToChange = condition
    barrier(10, needToChange) {
      node.put("barrierBroken", true)
    }
  }
}
