package it.unibo.cfsm

/** A functional module to manage the history of states in a CFSM. A history is represented as an abstract type H[_],
  * with operations to create, add to, and query the history. Next[S] is a transition to a state S with an associated
  * timestamp. The first state in the history is created with the `first` method. Histories can be combined using the
  * `max` method,
  * @see
  *   CollectiveFSM for an example of usage.
  */
trait HistoryModule {

  /** The type representing the history of states.
    * @tparam _
    *   the type of states in the history
    */
  type H[_]

  /** Creates a new history starting with the given initial state.
    * @param initial
    *   the initial state
    * @tparam S
    *   the type of states in the history
    * @return
    *   a new history containing the initial state
    */
  def createHistory[S: Ordering](initial: S): H[S]

  /** Creates the first Next state from a given state.
    * @param state
    *   the state to wrap
    * @tparam S
    *   the type of the state
    * @return
    *   a Next[S] instance representing the first state
    */
  def first[S: Ordering](state: S): Next[S]

  /** Adds a new state transition to the history.
    * @param history
    *   the current history
    * @param next
    *   the next state to add
    * @tparam S
    *   the type of states in the history
    * @return
    *   a new history with the added state
    */
  def add[S: Ordering](history: H[S], next: Next[S]): H[S]

  /** Gets the current state from the history.
    * @param history
    *   the history to query
    * @tparam S
    *   the type of states in the history
    * @return
    */
  def current[S: Ordering](history: H[S]): Next[S]

  /** Replaces the current state when a loop is detected in the history.
    * @param history
    *   the current history
    * @param next
    *   the next state to consider
    * @tparam S
    *   the type of states in the history
    * @return
    *   a new history with the replaced state if a loop was detected
    */
  def replaceWhenLooping[S: Ordering](history: H[S], next: Next[S]): H[S]

  /** Merges two histories, keeping the one with the maximum value according to the defined ordering.
    * @param h1
    *   the first history
    * @param h2
    *   the second history
    * @tparam S
    *   the type of states in the histories
    * @return
    *   the history with the maximum value
    */
  def max[S: Ordering](h1: H[S], h2: H[S]): H[S]

  /** Renders the history as a string representation.
    * @param history
    *   the history to render
    * @tparam S
    *   the type of states in the history
    * @return
    *   a string representation of the history
    */
  def render[S: Ordering](history: H[S]): String

  /** Computes the size of the history.
    * @param history
    *   the history to measure
    * @tparam S
    *   the type of states in the history
    * @return
    *   the size of the history in bytes
    */
  def size[S: Ordering](history: H[S]): Double

  /** Provides an ordering for histories based on their contents.
    * @tparam S
    *   the type of states in the histories
    * @return
    *   an ordering instance for histories
    */
  implicit def historyOrdering[S: Ordering]: Ordering[H[S]]

  /** Enriches the history type H[S] with additional operations.
    * @param history
    *   the history instance to extend
    * @param ordering$S$0
    *   the implicit ordering for type S
    * @tparam S
    *   the type of states in the history
    */
  implicit class HistoryOps[S: Ordering](val history: H[S]) {
    def add(next: Next[S]): H[S] = HistoryModule.this.add(history, next)
    def current: Next[S] = HistoryModule.this.current(history)
    def replaceWhenLooping(next: Next[S]): H[S] = HistoryModule.this.replaceWhenLooping(history, next)
  }
}
