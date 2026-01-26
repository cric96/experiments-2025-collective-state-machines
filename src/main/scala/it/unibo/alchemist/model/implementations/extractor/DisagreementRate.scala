package it.unibo.alchemist.model.implementations.extractor

import it.unibo.alchemist.boundary.Extractor
import it.unibo.alchemist.model.molecules.MoleculeConstants
import it.unibo.alchemist.model.{Actionable, Environment, Time}

import java.util
import scala.jdk.CollectionConverters.CollectionHasAsScala

/** An extractor that computes the disagreement rate among nodes in the environment. The disagreement rate is defined as
  * the ratio of pairs of nodes that have different states to the total number of pairs of nodes.
  */
class DisagreementRate extends Extractor[Double] {

  override def getColumnNames: util.List[String] = util.Arrays.asList("disagreementRate")

  override def extractData[T](
      environment: Environment[T, _],
      actionable: Actionable[T],
      time: Time,
      l: Long
  ): util.Map[String, Double] = {
    val nodes = environment.getNodes.asScala.toList
      .filter(_.contains(MoleculeConstants.STATE))
      .map(_.getConcentration(MoleculeConstants.STATE))
    val fullCombinations = combinationsWithRepetition(nodes, 2)
    val disagreements = fullCombinations.count { case List(a, b) => a != b }
    util.Collections.singletonMap(
      "disagreementRate",
      if (fullCombinations.isEmpty) 0.0
      else disagreements.toDouble / fullCombinations.size.toDouble
    )
  }

  def combinationsWithRepetition[T](list: List[T], r: Int): List[List[T]] = {
    if (r == 0) List(Nil)
    else
      for {
        i <- list.indices.toList
        tail <- combinationsWithRepetition(list.drop(i), r - 1)
      } yield list(i) :: tail
  }
}
