package it.unibo.cfsm

import it.unibo.cfsm.BoundedHistory.BoundedHistoryModule

class BoundedHistoryModuleTest extends HistoryModuleTest[BoundedHistoryModule](BoundedHistory.create(() => 0.0)) {

}
