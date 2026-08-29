package com.leanecorps.dapurjember.core.testing

import com.leanecorps.dapurjember.core.common.time.TimeProvider

/** A [TimeProvider] whose clock the test controls. */
class FakeTimeProvider(var now: Long = 0L) : TimeProvider {
    override fun nowMillis(): Long = now

    fun advanceBy(millis: Long) {
        now += millis
    }
}
