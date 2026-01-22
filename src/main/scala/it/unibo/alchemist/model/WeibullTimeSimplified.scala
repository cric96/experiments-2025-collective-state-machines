package it.unibo.alchemist.model
import it.unibo.alchemist.model.timedistributions.AbstractDistribution
import it.unibo.alchemist.model.times.DoubleTime
import org.apache.commons.math3.distribution.WeibullDistribution
import org.apache.commons.math3.random.RandomGenerator

/** A standard Weibull distribution for time events implemented in Scala.
  *
  * This class models a distribution where the provided parameters define the RATE (Frequency) of events. Therefore, the
  * next event time is calculated as 1.0 / sampled_value.
  *
  * @param shape
  *   (k) The shape parameter. k < 1: Failure rate decreases over time (infant mortality / bursty). k = 1: Constant
  *   failure rate (Exponential). k > 1: Failure rate increases over time (wear-out / regular timeout).
  * @param scale
  *   (lambda) The scale parameter.
  * @param offset
  *   Minimum offset applied to the sample.
  * @param start
  *   Initial time.
  * @param randomGenerator
  *   The random generator used internally.
  * @tparam T
  *   concentration type
  */
class WeibullTimeSimplified[T](
    val shape: Double,
    val scale: Double,
    val offset: Double,
    start: Time,
    randomGenerator: RandomGenerator
) extends AbstractDistribution[T](start) {

  // Direct instantiation of the Apache Commons Math distribution
  private val backingDistribution = new WeibullDistribution(
    randomGenerator,
    shape,
    scale,
    WeibullDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
  )

  /** Auxiliary constructor: Full parameters with default offset = 0.0 (Scala doesn't support default params in primary
    * constructor called from Java easily, so this helps if called from mixed context, otherwise primary handles it).
    */
  def this(shape: Double, scale: Double, start: Time, random: RandomGenerator) =
    this(shape, scale, 0.0, start, random)

  /** Auxiliary constructor: Simplified without explicit start time. Starts at DoubleTime(random * mean). */
  def this(mean: Double, deviation: Double, random: RandomGenerator) =
    this(
      mean,
      deviation,
      new DoubleTime(random.nextDouble() * mean),
      random
    )

  override def updateStatus(
      currentTime: Time,
      executed: Boolean,
      param: Double,
      environment: Environment[T, _]
  ): Unit = {
    if (executed) {
      // Generates a sample representing the RATE (Hz).
      val rateSample = genSample()

      // Avoid division by zero or extremely small numbers
      val interval = if (rateSample > 0) 1.0 / rateSample else Double.MaxValue

      this.setNextOccurrence(currentTime.plus(new DoubleTime(interval)))
    }
  }

  /** @return a sample from the distribution + offset */
  protected def genSample(): Double =
    backingDistribution.inverseCumulativeProbability(randomGenerator.nextDouble()) + this.offset

  override def getRate: Double = backingDistribution.getNumericalMean + offset

  override def cloneOnNewNode(destination: Node[T], currentTime: Time): WeibullTimeSimplified[T] =
    new WeibullTimeSimplified(shape, scale, offset, currentTime, randomGenerator)
}
