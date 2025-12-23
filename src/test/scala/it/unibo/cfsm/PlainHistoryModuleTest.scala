package it.unibo.cfsm

import it.unibo.cfsm.Next.AnyToNext

class PlainHistoryModuleTest extends HistoryModuleTest[PlainHistory.type](PlainHistory) {
  import module._
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

}
