package com.leanecorps.dapurjember.core.domain.printing

/**
 * Kicks the background job that drains the [PrintQueue]. Called right after a job is enqueued
 * so a ticket prints promptly, and once on app start to catch anything left from a crash.
 * The implementation is WorkManager-backed (architecture §6); the domain only needs the verb.
 */
interface PrintQueueScheduler {

    /** Request a one-off drain as soon as constraints allow. Safe to call repeatedly. */
    fun drainSoon()

    /** Register the periodic safety-net drain. Call once per process start. */
    fun ensurePeriodicDrain()
}
