package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.molecules.{MoleculeConstants, SimpleMolecule}
import it.unibo.alchemist.model._

import scala.jdk.CollectionConverters.CollectionHasAsScala

class DefendingBase[T, P <: Position[P]](
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
    new DefendingBase(node, environment, reaction, solvingRadius, nodesNeededToSolve)

  override def execute(): Unit = {
    if (node.getConcentration(MoleculeConstants.ATTACKED).asInstanceOf[Boolean]) {
      val size = environment.getNodesWithinRange(node, solvingRadius).size
      if (size >= nodesNeededToSolve) {
        val currentTime = environment.getSimulation.getTime.toDouble
        startTime match {
          case None =>
            startTime = Some(currentTime)
          case Some(start) if currentTime - start >= slackTime =>
            node.setConcentration(MoleculeConstants.ATTACKED, false.asInstanceOf[T])
            node.setConcentration(MoleculeConstants.DEFENDED, true.asInstanceOf[T])
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
