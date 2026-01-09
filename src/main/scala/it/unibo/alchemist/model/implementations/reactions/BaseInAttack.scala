package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.molecules.{MoleculeConstants, SimpleMolecule}
import it.unibo.alchemist.model.nodes.GenericNode
import it.unibo.alchemist.model.{Environment, Position, TimeDistribution}

class BaseInAttack[T, P <: Position[P]](
    environment: Environment[T, P],
    distribution: TimeDistribution[T]
) extends AbstractGlobalReaction[T, P](environment, distribution) {
  lazy val base = nodes.filter(_.contains(MoleculeConstants.BASE)).head

  override protected def executeBeforeUpdateDistribution(): Unit = {
    base.setConcentration(MoleculeConstants.ATTACKED, true.asInstanceOf[T])
    base.setConcentration(MoleculeConstants.DEFENDED, false.asInstanceOf[T])
  }
}
