package it.unibo.alchemist.model.molecules

import it.unibo.alchemist.model.Node
import it.unibo.program.CaseStudy

object MoleculeConstants {
  val IS_DRONE = new SimpleMolecule("drone")
  val IS_PROBLEM = new SimpleMolecule("problem")
  val IS_BASE = new SimpleMolecule("base")
  val BASE = new SimpleMolecule("base")
  val ALARM = new SimpleMolecule("alarm")
  val PROBLEM = new SimpleMolecule("problem")
  val ATTACKED = new SimpleMolecule("attacked")
  val DEFENDED = new SimpleMolecule("defended")
  val SOLVED = new SimpleMolecule("solved")
  val STATE = new SimpleMolecule("state")
  def updateState[T](node: Node[T], state: CaseStudy.MovementState): Unit = {
    CaseStudy.allCases.values.foreach(molecule =>
      node.setConcentration(new SimpleMolecule(molecule), false.asInstanceOf[T])
    )
    node.setConcentration(new SimpleMolecule(CaseStudy.allCases(state.getClass)), true.asInstanceOf[T])
  }
}
