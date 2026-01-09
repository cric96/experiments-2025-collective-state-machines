package it.unibo.macroswarm

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{AggregateProgram, BlocksWithGC, CustomSpawn, ScafiAlchemistSupport, StandardSensors, TimeUtils}
import it.unibo.cfsm.CollectiveFSM
import it.unibo.scafi.macroswarm.MacroSwarmAlchemistSupport.{BaseMovementLib, BlocksWithShare, FlockLib, GPSMovement, PatternFormationLib, ProcessFix}
import it.unibo.scafi.space.Point3D
import it.unibo.scafi.space.pimp._
trait MacroswarmFix {
  self:
    AggregateProgram with StandardSensors with TimeUtils
    with PatternFormationLib with BlocksWithShare with BlocksWithGC
    with ProcessFix with CustomSpawn with BaseMovementLib
    with FlockLib with ScafiAlchemistSupport with GPSMovement
    with CollectiveFSM =>

  override def goto(destination: Point3D, maxVelocity: Double = 1.0): Point3D = {
    val distance = currentPosition().distance(destination)
    val direction = (destination) - (currentPosition())
    val velocity = direction * (1 / distance)
    val scaledVelocity = math.min(distance, maxVelocity)
    if(distance < 1e-3) Point3D.Zero else
    velocity * scaledVelocity
  }
}
