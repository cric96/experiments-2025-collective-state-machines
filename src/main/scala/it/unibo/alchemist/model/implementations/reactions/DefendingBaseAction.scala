package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.molecules.MoleculeConstants
import it.unibo.alchemist.model._

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
    if (node.getConcentration(MoleculeConstants.ATTACKED).asInstanceOf[Boolean]) {
      runAfterQuorumForSlackTime {
        node.setConcentration(MoleculeConstants.ATTACKED, false.asInstanceOf[T])
        node.setConcentration(MoleculeConstants.DEFENDED, true.asInstanceOf[T])
      }
    }
  }

  override def getContext: Context = Context.NEIGHBORHOOD
}
