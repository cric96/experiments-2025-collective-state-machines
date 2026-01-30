package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.molecules.MoleculeConstants
import it.unibo.alchemist.model.nodes.GenericNode
import it.unibo.alchemist.model.reactions.Event
import it.unibo.alchemist.model.timedistributions.DiracComb
import it.unibo.alchemist.model.{Environment, Position, TimeDistribution}
import org.apache.commons.math3.random.RandomGenerator

/** A reaction that spawns a danger (problem) in the environment if there are no unsolved problems.
  *
  * @param environment
  *   the environment where the reaction takes place
  * @param distribution
  *   the time distribution of the reaction
  * @param randomGenerator
  *   the random generator used to generate problem positions
  * @param sideLength
  *   the side length of the area where problems can be spawned
  * @param visionProblemRadius
  *   the radius around the problem to consider for solving
  * @param nodesNeededToSolve
  *   the number of nodes needed to solve the problem
  * @param baseRadius
  *   the radius around the base where problems cannot be spawned
  * @tparam T
  *   the concentration type
  * @tparam P
  *   the position type
  */
class SpawnDanger[T, P <: Position[P]](
    environment: Environment[T, P],
    distribution: TimeDistribution[T],
    randomGenerator: RandomGenerator,
    sideLength: Double,
    visionProblemRadius: Double,
    nodesNeededToSolve: Int,
    baseRadius: Double
) extends AbstractGlobalReaction[T, P](environment, distribution) {
  lazy val base = nodes.filter(_.contains(MoleculeConstants.IS_BASE)).head

  override protected def executeBeforeUpdateDistribution(): Unit = {
    println("SpawnDanger triggered!" + environment.getSimulation.getTime)
    if (!anyProblemUnsolved) {
      val halfSize = sideLength / 2
      val x = (randomGenerator.nextDouble() * halfSize) + baseRadius
      val y = randomGenerator.nextDouble() * halfSize - halfSize / 2
      val problemPosition = Array(x, y)
      val dangerPosition = environment.makePosition(problemPosition)
      val dangerNode = new GenericNode(environment.getIncarnation, environment)
      dangerNode.setConcentration(MoleculeConstants.IS_PROBLEM, true.asInstanceOf[T])
      dangerNode.setConcentration(MoleculeConstants.READ, false.asInstanceOf[T])
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

  private def anyProblemUnsolved: Boolean = nodes.exists { node =>
    node.contains(MoleculeConstants.IS_PROBLEM)
  }
}
