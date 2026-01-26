package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.molecules.{MoleculeConstants, SimpleMolecule}
import it.unibo.alchemist.model.nodes.GenericNode
import it.unibo.alchemist.model.{Environment, Position, TimeDistribution}

/** A reaction that sets the base in attacked state.
  *
  * @param environment
  *   the environment where the reaction takes place
  * @param distribution
  *   the time distribution of the reaction
  * @tparam T
  *   the concentration type
  * @tparam P
  *   the position type
  */
class BaseInAttack[T, P <: Position[P]](
    environment: Environment[T, P],
    distribution: TimeDistribution[T]
) extends AbstractGlobalReaction[T, P](environment, distribution) {
  lazy val base = nodes.filter(_.contains(MoleculeConstants.IS_BASE)).head

  override protected def executeBeforeUpdateDistribution(): Unit = {
    base.setConcentration(MoleculeConstants.IS_ATTACKED, true.asInstanceOf[T])
    base.setConcentration(MoleculeConstants.DEFENDED, false.asInstanceOf[T])
  }
}
