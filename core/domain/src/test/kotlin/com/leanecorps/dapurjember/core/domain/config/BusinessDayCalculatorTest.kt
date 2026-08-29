package com.leanecorps.dapurjember.core.domain.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BusinessDayCalculatorTest {

    private val calc = BusinessDayCalculator()

    // 2026-08-29 17:05:00 UTC == 2026-08-30 00:05 in Asia/Jakarta (UTC+7)
    private val justAfterMidnight = 1_788_023_100_000L
    private val jakarta = "Asia/Jakarta"

    @Test
    fun `with no cutoff, just-after-midnight is the new day`() {
        assertEquals("2026-08-30", calc.businessDay(justAfterMidnight, cutoffMinutes = 0, zoneId = jakarta))
    }

    @Test
    fun `a 4am cutoff keeps a 00-05 order on the previous business day`() {
        val fourAm = 4 * 60
        assertEquals("2026-08-29", calc.businessDay(justAfterMidnight, cutoffMinutes = fourAm, zoneId = jakarta))
    }

    @Test
    fun `an unknown zone falls back to UTC without throwing`() {
        assertEquals("2026-08-29", calc.businessDay(justAfterMidnight, cutoffMinutes = 0, zoneId = "Nowhere/Nowhere"))
    }
}
