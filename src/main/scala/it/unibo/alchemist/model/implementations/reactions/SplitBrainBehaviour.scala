package it.unibo.alchemist.model.implementations.reactions

import it.unibo.alchemist.model.{Action, Actionable, Condition, Dependency, Environment, GlobalReaction, Node, Position, Time, TimeDistribution}
import org.danilopianini.util.{ListSet, ListSets}

import java.util
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.Random

class SplitBrainBehaviour[T, P <: Position[P]](
  environment: Environment[T, P],
  distribution: TimeDistribution[T],
  delta: Double
) extends AbstractGlobalReaction[T, P](environment,distribution) {

  override protected def executeBeforeUpdateDistribution(): Unit = {
    val (group1, group2) = nodes.splitAt(nodes.size / 2)
    def move(nodes: Seq[Node[T]], sign: Int): Unit = {
      for (node <- nodes) {
        val pos = environment.getPosition(node)
        val coords = pos.getCoordinates.clone()
        if (coords.nonEmpty) {
          coords(0) += (delta * sign)
          environment.moveNodeToPosition(node, environment.makePosition(coords))
        }
      }
    }
    move(group1, 1)
    move(group2, -1)
  }
}