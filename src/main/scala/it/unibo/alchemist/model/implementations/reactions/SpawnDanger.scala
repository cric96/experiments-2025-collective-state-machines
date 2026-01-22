package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.molecules.MoleculeConstants
import it.unibo.alchemist.model.nodes.GenericNode
import it.unibo.alchemist.model.reactions.Event
import it.unibo.alchemist.model.timedistributions.DiracComb
import it.unibo.alchemist.model.{Environment, Position, TimeDistribution}
import org.apache.commons.math3.random.RandomGenerator

class SpawnDanger[T, P <: Position[P]](
    environment: Environment[T, P],
    distribution: TimeDistribution[T],
    randomGenerator: RandomGenerator,
    sideLength: Double,
    visionProblemRadius: Double,
    nodesNeededToSolve: Int
) extends AbstractGlobalReaction[T, P](environment, distribution) {
  lazy val base = nodes.filter(_.contains(MoleculeConstants.BASE)).head

  override protected def executeBeforeUpdateDistribution(): Unit = {
    if (!anyProblemUnsolved) {
      val x = randomGenerator.nextDouble() * sideLength
      val y = randomGenerator.nextDouble() * sideLength - sideLength / 2
      val problemPosition = Array(x, y)
      val dangerPosition = environment.makePosition(problemPosition)
      val dangerNode = new GenericNode(environment.getIncarnation, environment)
      dangerNode.setConcentration(MoleculeConstants.PROBLEM, true.asInstanceOf[T])
      val reaction = new Event[T](
        dangerNode,
        new DiracComb(environment.getSimulation.getTime, 1)
      )
      reaction.setActions(
        java.util.List.of(
          new SolvingProblemAction[T, P](
            dangerNode, environment, reaction, visionProblemRadius, nodesNeededToSolve
          )
        )
      )
      dangerNode.addReaction(reaction)
      environment.addNode(dangerNode, dangerPosition)
    }
  }

  def anyProblemUnsolved: Boolean = nodes.exists { node =>
    node.contains(MoleculeConstants.PROBLEM)
  }
}
