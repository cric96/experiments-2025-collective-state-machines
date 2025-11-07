package it.unibo.program

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, ScafiAlchemistSupport, StandardSensors}

class ScafiProgram extends AggregateProgram with StandardSensors with ScafiAlchemistSupport {

  override def main(): Any = 10
}
