package com.leanecorps.dapurjember.core.domain.pricing

import com.leanecorps.dapurjember.core.common.money.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoundingRuleTest {

    @Test
    fun `NONE and NEAREST_1 leave the amount untouched`() {
        assertEquals(Money(37_437), RoundingRule.NONE.apply(Money(37_437)))
        assertEquals(Money(37_437), RoundingRule.NEAREST_1.apply(Money(37_437)))
    }

    @Test
    fun `NEAREST_100 rounds to the nearest hundred`() {
        assertEquals(Money(37_400), RoundingRule.NEAREST_100.apply(Money(37_437)))
        assertEquals(Money(37_500), RoundingRule.NEAREST_100.apply(Money(37_455)))
    }

    @Test
    fun `NEAREST_100 rounds an exact half away from zero`() {
        assertEquals(Money(12_400), RoundingRule.NEAREST_100.apply(Money(12_350)))
        assertEquals(Money(-12_400), RoundingRule.NEAREST_100.apply(Money(-12_350)))
    }

    @Test
    fun `NEAREST_5 rounds to the nearest five`() {
        assertEquals(Money(120), RoundingRule.NEAREST_5.apply(Money(122)))
        assertEquals(Money(125), RoundingRule.NEAREST_5.apply(Money(123)))
        assertEquals(Money(130), RoundingRule.NEAREST_5.apply(Money(128)))
    }
}
