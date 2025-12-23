package it.unibo.cfsm

import scala.math.Ordering.Implicits.seqOrdering

protected case class BoundedHistory[S: Ordering](
    wildcards: Int,
    transitions: List[(Double, Next[S])] = List()
)

object BoundedHistory {
  def create(timestampGenerator: TimeStampGenerator, maxLife: Double = Double.PositiveInfinity): BoundedHistoryModule =
    new BoundedHistoryModule(timestampGenerator, maxLife)

  trait TimeStampGenerator {
    def current(): Double
  }

  class BoundedHistoryModule(
      timestampGenerator: BoundedHistory.TimeStampGenerator,
      val maxLife: Double = Double.PositiveInfinity
  ) extends HistoryModule {
    type H[S] = BoundedHistory[S]

    def createHistory[S: Ordering](initial: S): BoundedHistory[S] =
      BoundedHistory(0, List((timestampGenerator.current(), first(initial))))

    def first[S: Ordering](state: S): Next[S] = Next(state, Double.PositiveInfinity)

    def add[S: Ordering](history: BoundedHistory[S], next: Next[S]): BoundedHistory[S] = {
      val timestamp = timestampGenerator.current()
      history.transitions match {
        case (_, last) :: (timestampPrevious, previous) :: tail
          if Next.same(last, previous) && Next.same(last, next) =>
          cleanUp(history.copy(transitions = (timestamp, next) :: timestampPrevious -> previous :: tail))
        case _ =>
          cleanUp(history.copy(transitions = (timestamp, next) :: history.transitions))
      }
    }

    def cleanUp[S: Ordering](history: BoundedHistory[S]): BoundedHistory[S] = {
      val currentTime = timestampGenerator.current()
      val cleanedTransitions = history.transitions.reverse.dropWhile { case (ts, _) => (currentTime - ts) > maxLife }.reverse
      val newTransitions = if (cleanedTransitions.size >= 2) {
        cleanedTransitions
      } else history.transitions.take(2)
      val wildcardsToAdd = history.transitions.size - newTransitions.size
      history.copy(history.wildcards + wildcardsToAdd, newTransitions)

    }

    def current[S: Ordering](history: BoundedHistory[S]): Next[S] =
      history.transitions.head._2

    def replaceWhenLooping[S: Ordering](history: BoundedHistory[S], next: Next[S]): BoundedHistory[S] = {
      history.transitions match {
        case (_, last) :: (timestampPrevious, previous) :: tail
          if Next.same(last, previous) && Next.same(next, last) =>
          val timestamp = timestampGenerator.current()
          cleanUp(history.copy(transitions = (timestamp, next) :: timestampPrevious -> previous :: tail))
        case _ => cleanUp(history)
      }
    }

    def max[S: Ordering](h1: BoundedHistory[S], h2: BoundedHistory[S]): BoundedHistory[S] = {
      if (historyOrdering[S].gt(h1, h2)) h1 else h2
    }

    def render[S: Ordering](history: BoundedHistory[S]): String = {
      s"BH (w = ${history.wildcards}) ~ ${(history.transitions.map { case (ts, s) => s"($s, $ts)" }).reverse.mkString(" -> ")}"
    }

    implicit def historyOrdering[S: Ordering]: Ordering[BoundedHistory[S]] = {
      val transitionsOrdering =
        Ordering.by[BoundedHistory[S], List[Next[S]]](_.transitions.map(_._2).reverse)

      (x: BoundedHistory[S], y: BoundedHistory[S]) => {
        val diff = x.wildcards - y.wildcards
        val (nx, ny) =
          if (diff == 0) (x, y)
          else if (diff > 0) (x, y.copy(transitions = y.transitions.drop(diff)))
          else (x.copy(transitions = x.transitions.drop(-diff)), y)
        transitionsOrdering.compare(nx, ny)
      }
    }
  }
}