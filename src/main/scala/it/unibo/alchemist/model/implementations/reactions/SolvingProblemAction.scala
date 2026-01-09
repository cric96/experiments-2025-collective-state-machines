package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.{Action, Context, Environment, Node, Position, Reaction}
import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.molecules.{MoleculeConstants, SimpleMolecule}

import scala.jdk.CollectionConverters.CollectionHasAsScala

class SolvingProblemAction[T, P <: Position[P]](
    node: Node[T],
    environment: Environment[T, P],
    reaction: Reaction[T],
    solvingRadius: Double,
    nodesNeededToSolve: Int
) extends AbstractAction[T](node) {
  private val slackTime = 60.0 // One minute
  private var startTime: Option[Double] = None

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
      val size = environment.getNodesWithinRange(node, solvingRadius).size
      if (size >= nodesNeededToSolve) {
        val currentTime = environment.getSimulation.getTime.toDouble
        startTime match {
          case None =>
            startTime = Some(currentTime)
          case Some(start) if currentTime - start >= slackTime =>
            node.setConcentration(MoleculeConstants.SOLVED, true.asInstanceOf[T])
            startTime = None
          case _ => // Still waiting
        }
      } else {
        startTime = None // Reset if conditions no longer met
      }
    }
  }

  override def getContext: Context = Context.NEIGHBORHOOD
}
