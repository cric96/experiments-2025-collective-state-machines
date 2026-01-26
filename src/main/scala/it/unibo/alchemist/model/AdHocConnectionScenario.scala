package it.unibo.alchemist.model

import it.unibo.alchemist.model.linkingrules.{AbstractLocallyConsistentLinkingRule, ConnectWithinDistance}
import it.unibo.alchemist.model.molecules.MoleculeConstants
import it.unibo.alchemist.model.neighborhoods.{Neighborhoods, SimpleNeighborhood}

import scala.jdk.CollectionConverters.{IterableHasAsJava, IteratorHasAsScala}

/** A linking rule connecting drones, problems, and base stations within different ranges. It connects:
  *   - Drones within `droneConnectionRange`
  *   - Problems within `problemConnectionRange`
  *   - Base stations within `baseConnectionRange`
  * @param droneConnectionRange
  *   the connection range for drones
  * @param problemConnectionRange
  *   the connection range for problems
  * @param baseConnectionRange
  *   the connection range for base stations
  * @tparam T
  *   the concentration type
  * @tparam P
  *   the position type
  */
class AdHocConnectionScenario[T, P <: Position[P]](
    droneConnectionRange: Double,
    problemConnectionRange: Double,
    baseConnectionRange: Double
) extends AbstractLocallyConsistentLinkingRule[T, P] {
  override def computeNeighborhood(center: Node[T], environment: Environment[T, P]): Neighborhood[T] = {
    val allDrones = environment
      .getNodesWithinRange(center, droneConnectionRange)
      .iterator()
      .asScala
      .filter(_.contains(MoleculeConstants.IS_DRONE))
      .toList
    val problems = environment
      .getNodesWithinRange(center, problemConnectionRange)
      .iterator()
      .asScala
      .filter(_.contains(MoleculeConstants.IS_PROBLEM))
      .toList
    val base = environment
      .getNodesWithinRange(center, baseConnectionRange)
      .iterator()
      .asScala
      .filter(_.contains(MoleculeConstants.IS_BASE))
      .toList
    Neighborhoods.make(environment, center, (allDrones ++ problems ++ base).asJava)
  }
}
