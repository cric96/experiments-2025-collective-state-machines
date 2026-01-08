package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.nodes.GenericNode
import it.unibo.alchemist.model.{Environment, Node, Position, TimeDistribution}

class SpawnDanger[T, P <: Position[P]](
  environment: Environment[T, P],
  distribution: TimeDistribution[T],
  delta: Double
) extends AbstractGlobalReaction[T, P](environment,distribution) {

  override protected def executeBeforeUpdateDistribution(): Unit = {
    val center = nodes.map(environment.getPosition).map(_.getCoordinates).transpose.map(coords => coords.sum / coords.size)
    val dangerPosition = environment.makePosition(center.toArray.map(_ + delta))
    val dangerNode = new GenericNode(environment.getIncarnation, environment)
    dangerNode.setConcentration(new SimpleMolecule("problem"), true.asInstanceOf[T])
    environment.addNode(dangerNode, dangerPosition)
  }
}