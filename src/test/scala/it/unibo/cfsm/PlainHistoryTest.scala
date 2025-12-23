package it.unibo.cfsm
import it.unibo.cfsm.Next._
import org.scalatest.funsuite._
import org.scalatest.matchers.should.Matchers
class PlainHistoryTest extends AnyFunSuite with Matchers {
  import PlainHistory._ // import operations
  test("History add method should not add duplicate states") {
    val initialState = "A"
    val firstTransition = first(initialState)
    val history = createHistory(initialState)
    val transition1 = Next("B", 1.0)
    val transition2 = Next("B", 2.0) // same state as state1 but different priority
    val transition3 = "C" --> 3.0

    val updatedHistory1 = history.add(transition1)
    updatedHistory1.transitions.head shouldBe transition1
    updatedHistory1.transitions shouldBe transition1 :: firstTransition :: Nil
    val updatedHistory2 = updatedHistory1.add(transition2)
    updatedHistory2.transitions.head shouldBe transition2
    updatedHistory2.transitions shouldBe transition2 :: transition1 :: firstTransition :: Nil
    val updatedHistory3 = updatedHistory2.add(transition3)
    updatedHistory3.transitions.head shouldBe transition3
    updatedHistory3.transitions shouldBe transition3 :: transition1 :: firstTransition :: Nil // state 2 was the self loop
  }

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