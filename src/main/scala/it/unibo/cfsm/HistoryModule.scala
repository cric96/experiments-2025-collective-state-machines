package it.unibo.cfsm

trait HistoryModule {

  type H[_]
  def createHistory[S: Ordering](initial: S): H[S]
  def first[S: Ordering](state: S): Next[S]
  def add[S: Ordering](history: H[S], next: Next[S]): H[S]
  def current[S: Ordering](history: H[S]): Next[S]
  def replaceWhenLooping[S: Ordering](history: H[S], next: Next[S]): H[S]
  def max[S: Ordering](h1: H[S], h2: H[S]): H[S]
  def render[S: Ordering](history: H[S]): String
  // Returns the size (in bytes) of the history representation
  def size[S: Ordering](history: H[S]): Double
  implicit def historyOrdering[S: Ordering]: Ordering[H[S]]

  implicit class HistoryOps[S: Ordering](val history: H[S]) {
    def add(next: Next[S]): H[S] = HistoryModule.this.add(history, next)
    def current: Next[S] = HistoryModule.this.current(history)
    def replaceWhenLooping(next: Next[S]): H[S] = HistoryModule.this.replaceWhenLooping(history, next)
  }
}
