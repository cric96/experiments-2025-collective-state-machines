package it.unibo.program

import it.unibo.alchemist.model.{Environment, Molecule, Node, Position}
import it.unibo.alchemist.model.molecules.MoleculeConstants
import it.unibo.scafi.space.Point3D

import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsScala}

object ExternalSensors {

  private def nodesWithinRange(environment: Environment[Any, Position[_]], nodeId: Int): Iterable[Node[Any]] =
    environment.getNeighborhood(environment.getNodeByID(nodeId)).asScala

  private def findNodeWithMolecule(
      environment: Environment[Any, Position[_]],
      nodeId: Int,
      molecule: Molecule
  ): Option[Node[Any]] =
    nodesWithinRange(environment, nodeId).find(_.contains(molecule))

  private def moleculeConcentration[T](node: Node[Any], molecule: Molecule): T =
    node.getConcentration(molecule).asInstanceOf[T]
  private def writeMoleculeConcentration[T](node: Node[Any], molecule: Molecule, value: T): Unit =
    node.setConcentration(molecule, value.asInstanceOf[Any])
  private def nodePosition(environment: Environment[Any, Position[_]], node: Node[Any]): Point3D = {
    val position = environment.getPosition(node)
    Point3D(position.getCoordinate(0), position.getCoordinate(1), 0.0)
  }

  def anyAlarm(environment: Environment[Any, Position[_]], nodeId: Int): Boolean =
    findNodeWithMolecule(environment, nodeId, MoleculeConstants.IS_ALARM)
      .exists(moleculeConcentration[Boolean](_, MoleculeConstants.IS_ALARM))

  def anyProblemFound(environment: Environment[Any, Position[_]], nodeId: Int): Boolean =
    nodesWithinRange(environment, nodeId).exists(_.contains(MoleculeConstants.IS_PROBLEM))

  def positionOfProblem(environment: Environment[Any, Position[_]], nodeId: Int): Option[Point3D] =
    findNodeWithMolecule(environment, nodeId, MoleculeConstants.IS_PROBLEM)
      .map(nodePosition(environment, _))

  def baseAttacked(environment: Environment[Any, Position[_]], nodeId: Int): Boolean =
    findNodeWithMolecule(environment, nodeId, MoleculeConstants.IS_BASE)
      .exists(moleculeConcentration[Boolean](_, MoleculeConstants.IS_ATTACKED))

  def baseDefended(environment: Environment[Any, Position[_]], nodeId: Int): Boolean =
    findNodeWithMolecule(environment, nodeId, MoleculeConstants.IS_BASE)
      .exists(moleculeConcentration[Boolean](_, MoleculeConstants.DEFENDED))

  def isSolved(environment: Environment[Any, Position[_]], nodeId: Int): Boolean = {
    val result = nodesWithinRange(environment, nodeId).find(_.contains(MoleculeConstants.SOLVED))
    result match {
      case Some(node) =>
        writeMoleculeConcentration(node, MoleculeConstants.READ, true)
        val base = environment.getNodes.asScala.toList
          .filter(_.contains(MoleculeConstants.IS_BASE))
          .foreach(b => writeMoleculeConcentration(b, MoleculeConstants.IS_ALARM, false))
      case None => ()
    }
    result.nonEmpty
  }
}
