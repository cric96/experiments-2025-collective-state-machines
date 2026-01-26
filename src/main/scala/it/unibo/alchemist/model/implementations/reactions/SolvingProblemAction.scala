package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.molecules.MoleculeConstants
import it.unibo.alchemist.model._

import scala.jdk.CollectionConverters.CollectionHasAsScala

/** An action that marks a problem as solved when enough neighboring nodes have also solved it, and removes the node
  * from the environment if it has read the solution.
  * @param node
  *   the node this action is associated with
  * @param environment
  *   the environment in which the node exists
  * @param reaction
  * @param solvingRadius
  *   the radius within which to count neighboring nodes
  * @param nodesNeededToSolve
  *   the number of nodes required within the radius to trigger the action
  * @tparam T
  *   the concentration type
  * @tparam P
  *   the position type
  */
class SolvingProblemAction[T, P <: Position[P]](
    node: Node[T],
    override protected val environment: Environment[T, P],
    reaction: Reaction[T],
    override protected val solvingRadius: Double,
    override protected val nodesNeededToSolve: Int
) extends TimedQuorumAction[T, P](node, environment, solvingRadius, nodesNeededToSolve) {
  private lazy val base =
    environment.getNodes.asScala.toList.filter(_.contains(MoleculeConstants.IS_BASE)).head

  override def cloneAction(node: Node[T], reaction: Reaction[T]): Action[T] =
    new SolvingProblemAction(node, environment, reaction, solvingRadius, nodesNeededToSolve)

  override def execute(): Unit = {
    if (
      node.getConcentration(MoleculeConstants.SOLVED).asInstanceOf[Boolean] && node
        .getConcentration(MoleculeConstants.READ)
        .asInstanceOf[Boolean]
    ) {
      node.removeReaction(reaction)
      environment.getSimulation.reactionRemoved(reaction)
      environment.removeNode(node)
      base.setConcentration(MoleculeConstants.IS_ALARM, false.asInstanceOf[T])
    } else {
      runAfterQuorumForSlackTime {
        node.setConcentration(MoleculeConstants.SOLVED, true.asInstanceOf[T])
      }
    }
  }

  override def getContext: Context = Context.NEIGHBORHOOD
}
