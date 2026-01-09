package it.unibo.program

import it.unibo.alchemist.model.{Environment, Molecule, Node, Position}
import it.unibo.alchemist.model.molecules.MoleculeConstants
import it.unibo.scafi.space.Point3D

import scala.jdk.CollectionConverters.CollectionHasAsScala

object ExternalSensors {

  private def nodesWithinRange(environment: Environment[Any, Position[_]], nodeId: Int, range: Double): Iterable[Node[Any]] =
    environment.getNodesWithinRange(environment.getNodeByID(nodeId), range).asScala

  private def findNodeWithMolecule(
      environment: Environment[Any, Position[_]],
      nodeId: Int,
      molecule: Molecule,
      range: Double
  ): Option[Node[Any]] =
    nodesWithinRange(environment, nodeId, range).find(_.contains(molecule))

  private def moleculeConcentration[T](node: Node[Any], molecule: Molecule): T =
    node.getConcentration(molecule).asInstanceOf[T]

  private def nodePosition(environment: Environment[Any, Position[_]], node: Node[Any]): Point3D = {
    val position = environment.getPosition(node)
    Point3D(position.getCoordinate(0), position.getCoordinate(1), 0.0)
  }

  def anyAlarm(environment: Environment[Any, Position[_]], nodeId: Int, visionRange: Double): Boolean =
    findNodeWithMolecule(environment, nodeId, MoleculeConstants.ALARM, visionRange)
      .exists(moleculeConcentration[Boolean](_, MoleculeConstants.ALARM))

  def anyProblemFound(environment: Environment[Any, Position[_]], nodeId: Int, visionRange: Double): Boolean =
    nodesWithinRange(environment, nodeId, visionRange).exists(_.contains(MoleculeConstants.PROBLEM))

  def positionOfProblem(environment: Environment[Any, Position[_]], nodeId: Int, visionRange: Double): Option[Point3D] =
    findNodeWithMolecule(environment, nodeId, MoleculeConstants.PROBLEM, visionRange)
      .map(nodePosition(environment, _))

  def baseAttacked(environment: Environment[Any, Position[_]], nodeId: Int, visionRange: Double): Boolean =
    findNodeWithMolecule(environment, nodeId, MoleculeConstants.BASE, visionRange)
      .exists(moleculeConcentration[Boolean](_, MoleculeConstants.ATTACKED))

  def baseDefended(environment: Environment[Any, Position[_]], nodeId: Int, visionRange: Double): Boolean =
    findNodeWithMolecule(environment, nodeId, MoleculeConstants.BASE, visionRange)
      .exists(moleculeConcentration[Boolean](_, MoleculeConstants.DEFENDED))

  def isSolved(environment: Environment[Any, Position[_]], nodeId: Int, visionRange: Double): Boolean =
    nodesWithinRange(environment, nodeId, visionRange).exists(_.contains(MoleculeConstants.SOLVED))
}
