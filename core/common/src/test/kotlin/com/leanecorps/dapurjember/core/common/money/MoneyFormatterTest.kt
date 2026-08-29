package com.leanecorps.dapurjember.core.common.money

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MoneyFormatterTest {

    @Test
    fun `formats zero-minor-unit currency with grouping`() {
        assertEquals("IDR 1,500,000", formatMoney(Money(1_500_000), "IDR", 0))
        assertEquals("IDR 15,000", formatMoney(Money(15_000), "IDR", 0))
    }

    @Test
    fun `formats two-minor-unit currency`() {
        assertEquals("USD 1,234.56", formatMoney(Money(123_456), "USD", 2))
        assertEquals("USD 0.05", formatMoney(Money(5), "USD", 2))
    }

    @Test
    fun `formats zero`() {
        assertEquals("IDR 0", formatMoney(Money.ZERO, "IDR", 0))
        assertEquals("USD 0.00", formatMoney(Money.ZERO, "USD", 2))
    }

    @Test
    fun `formats negative amounts`() {
        assertEquals("USD -1,234.56", formatMoney(Money(-123_456), "USD", 2))
        assertEquals("IDR -2,000", formatMoney(Money(-2_000), "IDR", 0))
    }

    @Test
    fun `formatAmount drops the currency code`() {
        assertEquals("1,500,000", formatAmount(Money(1_500_000), 0))
        assertEquals("1,234.56", formatAmount(Money(123_456), 2))
        assertEquals("0.05", formatAmount(Money(5), 2))
        assertEquals("-2,000", formatAmount(Money(-2_000), 0))
    }
}
