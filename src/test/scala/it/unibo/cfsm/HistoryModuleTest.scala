package it.unibo.cfsm
import org.scalatest.funsuite._
import org.scalatest.matchers.should.Matchers
abstract class HistoryModuleTest[M <: HistoryModule](val module: M) extends AnyFunSuite with Matchers {
  import module._ // import operations

  test("History current method should return the latest state") {
    val initialState = "A"
    val history = createHistory(initialState)
    val transition1 = Next("B", 1.0)
    val transition2 = Next("C", 2.0)
    val updatedHistory = history.add(transition1).add(transition2)
    updatedHistory.current shouldBe transition2
  }

  test("History max should work in the lexicographical order") {
    val history1 = createHistory(0).add(Next(0, 0)).add(Next(1, 1.0)).add(Next(2, 1.0))
    val history2 = createHistory(0).add(Next(0, 0)).add(Next(1, 1.0))
    val history3 = createHistory(0).add(Next(0, 0))
    val histories = List(history1, history2, history3)
    val maxHistory = histories.max
     maxHistory shouldBe history1
  }
}