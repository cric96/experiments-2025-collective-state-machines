package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.{Environment, Node, Position}

abstract class TimedQuorumAction[T, P <: Position[P]](
    node: Node[T],
    protected val environment: Environment[T, P],
    protected val solvingRadius: Double,
    protected val nodesNeededToSolve: Int
) extends AbstractAction[T](node) {
  protected val slackTime: Double = 60.0 // One minute
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
