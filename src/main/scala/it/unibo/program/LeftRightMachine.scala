package it.unibo.program

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, ScafiAlchemistSupport, StandardSensors}
import it.unibo.cfsm.CollectiveFSM
import it.unibo.cfsm.Next.AnyToNext

class LeftRightMachine extends AggregateProgram
  with StandardSensors
  with ScafiAlchemistSupport
  with CollectiveFSM {

  val waitState = 0
  val leftSide = 1
  val rightSide = 2

  override def main(): Any = {
    val state = cfsm(waitState) { current =>
      if(current == waitState) {
        if(senseLeft) {
          leftSide --> 2.0
        } else if(senseRight) {
          rightSide --> 1.0
        } else {
          waitState
        }
      } else {
        current --> Double.MinValue
      }
    }
    node.put("state", state)
  }

  def senseLeft: Boolean = sense[Boolean]("left")
  def senseRight: Boolean = sense[Boolean]("right")
}
