package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.times.DoubleTime
import it.unibo.alchemist.model.{Environment, Node, Time, TimeDistribution}

import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.jdk.CollectionConverters.ListHasAsScala

class PredefinedTimeDistribution[T](times: java.util.List[Double]) extends TimeDistribution[T] {

  private var remainingTimes: List[Time] = times.asScala.toList.sorted.map(new DoubleTime(_))
  private var nextOccurrence: Time = if (remainingTimes.nonEmpty) {
    remainingTimes.head
  } else {
    new DoubleTime(Double.PositiveInfinity)
  }

  override def update(currentTime: Time,executed: Boolean,param: Double, environment: Environment[T, _]): Unit = {
    if (executed && remainingTimes.nonEmpty) {
      remainingTimes = remainingTimes.tail
      nextOccurrence = if (remainingTimes.nonEmpty) {
        remainingTimes.head
      } else {
        new DoubleTime(Double.PositiveInfinity)
      }
    }
  }

  override def getNextOccurence: Time = nextOccurrence

  override def getRate: Double = Double.NaN

  override def cloneOnNewNode(destination: Node[T], currentTime: Time): TimeDistribution[T] = {
    new PredefinedTimeDistribution[T](remainingTimes.map(_.toDouble).asJava)
  }
}
