package com.leanecorps.dapurjember.core.common.time

/**
 * The current wall-clock time as UTC epoch milliseconds. Injected everywhere a timestamp is
 * written so tests can pin the clock. Timestamps are stored UTC and rendered in the store's
 * configured zone.
 */
fun interface TimeProvider {
    fun nowMillis(): Long
}

/** Production [TimeProvider] backed by the system clock. */
class SystemTimeProvider : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
