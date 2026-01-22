package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.{Action, Context, Environment, Node, Position, Reaction}
import it.unibo.alchemist.model.molecules.MoleculeConstants

import scala.jdk.CollectionConverters.CollectionHasAsScala

class SolvingProblemAction[T, P <: Position[P]](
    node: Node[T],
    override protected val environment: Environment[T, P],
    reaction: Reaction[T],
    override protected val solvingRadius: Double,
    override protected val nodesNeededToSolve: Int
) extends TimedQuorumAction[T, P](node, environment, solvingRadius, nodesNeededToSolve) {

  lazy val base =
    environment.getNodes.asScala.toList.filter(_.contains(MoleculeConstants.BASE)).head

  override def cloneAction(node: Node[T], reaction: Reaction[T]): Action[T] =
    new SolvingProblemAction(node, environment, reaction, solvingRadius, nodesNeededToSolve)

  override def execute(): Unit = {
    if (node.getConcentration(MoleculeConstants.SOLVED).asInstanceOf[Boolean]) {
      node.removeReaction(reaction)
      environment.getSimulation.reactionRemoved(reaction)
      environment.removeNode(node)
      base.setConcentration(MoleculeConstants.ALARM, false.asInstanceOf[T])
    } else {
      runAfterQuorumForSlackTime {
        node.setConcentration(MoleculeConstants.SOLVED, true.asInstanceOf[T])
      }
    }
  }

  override def getContext: Context = Context.NEIGHBORHOOD
}
