package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.molecules.MoleculeConstants
import it.unibo.alchemist.model.{Environment, Position, TimeDistribution}

/** A reaction that sets the alarm state of the base to true.
  * @param environment
  *   the environment where the reaction takes place
  * @param distribution
  *   the time distribution of the reaction
  * @tparam T
  *   the concentration type
  * @tparam P
  *   the position type
  */
class Alarm[T, P <: Position[P]](
    environment: Environment[T, P],
    distribution: TimeDistribution[T]
) extends AbstractGlobalReaction[T, P](environment, distribution) {
  private lazy val base = nodes.filter(_.contains(MoleculeConstants.IS_BASE)).head

  override protected def executeBeforeUpdateDistribution(): Unit = {
    println("Alarm triggered!" + environment.getSimulation.getTime)
    base.setConcentration(MoleculeConstants.IS_ALARM, true.asInstanceOf[T])
  }
}
