package com.leanecorps.dapurjember.core.domain.config

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Computes the reporting business day for a timestamp (`docs/2-architecture` §8): an order
 * opened at 23:55 and paid at 00:05 belongs to the same day. The cutoff is
 * `store_profile.business_day_cutoff_min` minutes past midnight in the store's zone.
 */
class BusinessDayCalculator @Inject constructor() {

    /** `YYYY-MM-DD` for [epochMillis], shifting back by [cutoffMinutes] before taking the date. */
    fun businessDay(epochMillis: Long, cutoffMinutes: Int, zoneId: String): String {
        val zone = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.of("UTC"))
        val shifted = Instant.ofEpochMilli(epochMillis).minusSeconds(cutoffMinutes.toLong() * SECONDS_PER_MINUTE)
        return LocalDate.ofInstant(shifted, zone).toString()
    }

    fun businessDay(epochMillis: Long, profile: StoreProfile): String =
        businessDay(epochMillis, profile.businessDayCutoffMinutes, profile.timezoneId)

    private companion object {
        const val SECONDS_PER_MINUTE = 60L
    }
}
