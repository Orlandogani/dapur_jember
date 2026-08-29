package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.domain.printing.PrintQueueScheduler

/** Counts the drain requests instead of touching WorkManager. */
class FakePrintQueueScheduler : PrintQueueScheduler {

    var drainSoonCalls = 0
        private set
    var periodicCalls = 0
        private set

    override fun drainSoon() {
        drainSoonCalls++
    }

    override fun ensurePeriodicDrain() {
        periodicCalls++
    }
}
