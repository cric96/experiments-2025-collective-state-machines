package it.unibo.fsm
import it.unibo.fsm.State._
import org.scalatest.funsuite._
import org.scalatest.matchers.should.Matchers
class HistoryTest extends AnyFunSuite with Matchers {
  test("History add method should not add duplicate states") {
    val initialState = "A"
    val history = History(initialState)
    val state1 = State("B", 1.0)
    val state2 = State("B", 2.0) // same state as state1 but different priority
    val state3 = "C" --> 3.0

    val updatedHistory1 = history.add(state1)
    updatedHistory1.states.head shouldBe state1
    val updatedHistory2 = updatedHistory1.add(state2)
    updatedHistory2.states.head shouldBe state2 // should replace state1
    val updatedHistory3 = updatedHistory2.add(state3)
    updatedHistory3.states.head shouldBe state3
    updatedHistory3.states(1) shouldBe state2
  }

  test("History current method should return the latest state") {
    val initialState = "A"
    val history = History(initialState)
    val state1 = State("B", 1.0)
    val state2 = State("C", 2.0)
    val updatedHistory = history.add(state1).add(state2)
    updatedHistory.current shouldBe state2
  }

  test("History max should work in the lexicographical order") {
    val history1 = History(0).add(State(0, 0)).add(State(1, 1.0)).add(State(0, 1.0))
    val history2 = History(0).add(State(0, 0)).add(State(1, 1.0))
    val history3 = History(0).add(State(0, 0))
    val histories = List(history1, history2, history3)
    val maxHistory = History.max(histories)
    maxHistory shouldBe history1
  }
}