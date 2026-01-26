package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.molecules.MoleculeConstants
import it.unibo.alchemist.model._

/** An action used to simulate the defending of a base. A base is defended if it was previously attacked and when the
  * nodes stay around it for a certain amount of time.
  * @param node
  *   the base node which checks if it is being defended
  * @param environment
  *   the environment where the node is located
  * @param reaction
  *   the reaction that contains this action
  * @param solvingRadius
  *   the radius around the node to consider for the quorum
  * @param nodesNeededToSolve
  *   the number of nodes needed to form the quorum
  * @tparam T
  *   the concentration type
  * @tparam P
  *   the position type
  */
class DefendingBaseAction[T, P <: Position[P]](
    node: Node[T],
    override protected val environment: Environment[T, P],
    reaction: Reaction[T],
    override protected val solvingRadius: Double,
    override protected val nodesNeededToSolve: Int
) extends TimedQuorumAction[T, P](node, environment, solvingRadius, nodesNeededToSolve) {

  override def cloneAction(node: Node[T], reaction: Reaction[T]): Action[T] =
    new DefendingBaseAction(node, environment, reaction, solvingRadius, nodesNeededToSolve)

  override def execute(): Unit = {
    if (node.getConcentration(MoleculeConstants.IS_ATTACKED).asInstanceOf[Boolean]) {
      runAfterQuorumForSlackTime {
        node.setConcentration(MoleculeConstants.IS_ATTACKED, false.asInstanceOf[T])
        node.setConcentration(MoleculeConstants.DEFENDED, true.asInstanceOf[T])
      }
    }
  }

  override def getContext: Context = Context.NEIGHBORHOOD
}
