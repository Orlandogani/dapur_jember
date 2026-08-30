package com.leanecorps.dapurjember.core.domain.reports

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.order.PaymentMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReportCsvTest {

    private val summary = DailySummary(
        businessDay = "2026-08-30",
        orderCount = 2,
        covers = 5,
        grossRevenue = Money(100_000),
        cogs = Money(30_000),
        paymentMix = listOf(PaymentMixRow(PaymentMethod.CASH, Money(100_000))),
        discountCount = 1,
        discountTotal = Money(5_000),
        voidedOrders = 0,
        voidedLines = 1,
    )

    private fun csv(minorUnits: Int = 0) = ReportCsv.dailySummary(
        summary = summary,
        items = listOf(ItemSales("Nasi Goreng", 4, Money(60_000), Money(18_000))),
        categories = listOf(CategorySales("Rice", 4, Money(60_000))),
        audit = listOf(
            AuditEntry(AuditKind.VOID, "Sari", "Es Teh", "wrong order", Money(5_000), 1L),
        ),
        currencyMinorUnits = minorUnits,
    )

    @Test
    fun `includes the summary, item, category and audit sections`() {
        val out = csv()
        assertTrue(out.contains("Gross revenue,100000"))
        assertTrue(out.contains("COGS,30000"))
        assertTrue(out.contains("Gross profit,70000"))
        assertTrue(out.contains("Gross margin %,70.0"))
        assertTrue(out.contains("CASH,100000"))
        assertTrue(out.contains("Nasi Goreng,4,60000,18000,42000,70.0"))
        assertTrue(out.contains("Rice,4,60000"))
        assertTrue(out.contains("VOID,Sari,Es Teh,wrong order,5000"))
    }

    @Test
    fun `money is rendered in major units at the store scale`() {
        assertTrue(csv(minorUnits = 2).contains("Gross revenue,1000.00"))
    }

    @Test
    fun `an unknown margin is left blank rather than written as zero`() {
        val out = ReportCsv.dailySummary(
            summary = summary.copy(grossRevenue = Money.ZERO, cogs = Money.ZERO, orderCount = 0),
            items = emptyList(),
            categories = emptyList(),
            audit = emptyList(),
            currencyMinorUnits = 0,
        )
        assertTrue(out.contains("Gross margin %,\n"))
    }

    @Test
    fun `values containing a comma, quote or newline are RFC 4180 quoted`() {
        assertEquals("plain", ReportCsv.escape("plain"))
        assertEquals("\"a,b\"", ReportCsv.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", ReportCsv.escape("say \"hi\""))
        assertEquals("\"two\nlines\"", ReportCsv.escape("two\nlines"))
    }

    @Test
    fun `an item name with a comma cannot break the column layout`() {
        val out = ReportCsv.dailySummary(
            summary = summary,
            items = listOf(ItemSales("Rice, fried", 1, Money(10_000), Money.ZERO)),
            categories = emptyList(),
            audit = emptyList(),
            currencyMinorUnits = 0,
        )
        assertTrue(out.contains("\"Rice, fried\",1,10000"))
    }
}
