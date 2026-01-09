package it.unibo.alchemist.model.molecules

import it.unibo.alchemist.model.Node
import it.unibo.program.CaseStudy

object MoleculeConstants {
  val BASE = new SimpleMolecule("base")
  val ALARM = new SimpleMolecule("alarm")
  val PROBLEM = new SimpleMolecule("problem")
  val ATTACKED = new SimpleMolecule("attacked")
  val DEFENDED = new SimpleMolecule("defended")
  val SOLVED = new SimpleMolecule("solved")
  def updateState[T](node: Node[T], state: CaseStudy.MovementState): Unit = {
    CaseStudy.allCases.values.foreach(molecule =>
      node.setConcentration(new SimpleMolecule(molecule), false.asInstanceOf[T])
    )
    node.setConcentration(new SimpleMolecule(CaseStudy.allCases(state.getClass)), true.asInstanceOf[T])
  }
}
