package com.leanecorps.dapurjember.core.domain.reports

import com.leanecorps.dapurjember.core.common.money.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DailySummaryTest {

    private fun summary(revenueMinor: Long, cogsMinor: Long, orders: Int = 1) = DailySummary(
        businessDay = "2026-08-30",
        orderCount = orders,
        covers = 2,
        grossRevenue = Money(revenueMinor),
        cogs = Money(cogsMinor),
        paymentMix = emptyList(),
        discountCount = 0,
        discountTotal = Money.ZERO,
        voidedOrders = 0,
        voidedLines = 0,
    )

    @Test
    fun `gross profit is revenue minus cogs and margin is a percentage of revenue`() {
        val s = summary(revenueMinor = 100_000, cogsMinor = 30_000)
        assertEquals(Money(70_000), s.grossProfit)
        assertEquals(70.0, s.grossMarginPercent!!, 0.001)
    }

    @Test
    fun `a day with no revenue reports an unknown margin, not zero percent`() {
        assertNull(summary(revenueMinor = 0, cogsMinor = 0, orders = 0).grossMarginPercent)
    }

    @Test
    fun `selling below cost yields a negative margin rather than being clamped`() {
        val s = summary(revenueMinor = 10_000, cogsMinor = 15_000)
        assertEquals(Money(-5_000), s.grossProfit)
        assertEquals(-50.0, s.grossMarginPercent!!, 0.001)
    }

    @Test
    fun `average ticket divides revenue across paid orders and is zero with none`() {
        assertEquals(Money(25_000), summary(100_000, 0, orders = 4).averageTicket)
        assertEquals(Money.ZERO, summary(0, 0, orders = 0).averageTicket)
    }

    @Test
    fun `item margin is null without revenue and correct with it`() {
        assertNull(ItemSales("Free sample", 1, Money.ZERO, Money(500)).marginPercent)
        val sold = ItemSales("Nasi Goreng", 2, Money(30_000), Money(9_000))
        assertEquals(Money(21_000), sold.profit)
        assertEquals(70.0, sold.marginPercent!!, 0.001)
    }
}
