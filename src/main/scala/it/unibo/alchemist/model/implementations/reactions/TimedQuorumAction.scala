package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.{Environment, Node, Position}

/** An action that waits for a quorum of nodes within a certain radius for a specified slack time before executing a
  * given operation.
  *
  * @param node
  *   the node this action is associated with
  * @param environment
  *   the environment in which the node exists
  * @param solvingRadius
  *   the radius within which to count neighboring nodes
  * @param nodesNeededToSolve
  *   the number of nodes required within the radius to trigger the action
  * @tparam T
  *   the concentration type
  * @tparam P
  *   the position type
  */
abstract class TimedQuorumAction[T, P <: Position[P]](
    node: Node[T],
    protected val environment: Environment[T, P],
    protected val solvingRadius: Double,
    protected val nodesNeededToSolve: Int
) extends AbstractAction[T](node) {
  protected val slackTime: Double = 60.0 // One minute, can be made configurable if needed
  private var startTime: Option[Double] = None

  final protected def runAfterQuorumForSlackTime(onReady: => Unit): Unit = {
    val size = environment.getNodesWithinRange(node, solvingRadius).size
    if (size >= nodesNeededToSolve) {
      val currentTime = environment.getSimulation.getTime.toDouble
      startTime match {
        case None =>
          startTime = Some(currentTime)
        case Some(start) if currentTime - start >= slackTime =>
          onReady
          startTime = None
        case _ => // Still waiting
      }
    } else {
      startTime = None // Reset if conditions no longer met
    }
  }
}
