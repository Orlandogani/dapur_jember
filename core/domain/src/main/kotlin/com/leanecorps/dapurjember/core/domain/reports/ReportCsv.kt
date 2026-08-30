package com.leanecorps.dapurjember.core.domain.reports

import com.leanecorps.dapurjember.core.common.money.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Renders a day's reports as CSV for the share sheet (FR-R3). Pure and Android-free so the
 * exact output is unit-testable; the feature layer only writes the string to a file.
 *
 * Money is emitted in **major units** with the store's scale (`25000` minor at scale 0 →
 * `25000`; `350` minor at scale 2 → `3.50`) so the file opens sensibly in a spreadsheet.
 */
object ReportCsv {

    fun dailySummary(
        summary: DailySummary,
        items: List<ItemSales>,
        categories: List<CategorySales>,
        audit: List<AuditEntry>,
        currencyMinorUnits: Int,
    ): String {
        val money = { m: Money -> majorUnits(m, currencyMinorUnits) }
        return buildString {
            appendRow("Daily summary", summary.businessDay)
            appendRow("Gross revenue", money(summary.grossRevenue))
            appendRow("COGS", money(summary.cogs))
            appendRow("Gross profit", money(summary.grossProfit))
            appendRow("Gross margin %", summary.grossMarginPercent?.let { format1dp(it) } ?: "")
            appendRow("Paid orders", summary.orderCount.toString())
            appendRow("Covers", summary.covers.toString())
            appendRow("Average ticket", money(summary.averageTicket))
            appendRow("Discounts", summary.discountCount.toString(), money(summary.discountTotal))
            appendRow("Voided orders", summary.voidedOrders.toString())
            appendRow("Voided lines", summary.voidedLines.toString())

            appendLine()
            appendRow("Payment method", "Amount")
            summary.paymentMix.forEach { appendRow(it.method.name, money(it.amount)) }

            appendLine()
            appendRow("Item", "Qty", "Gross", "Cost", "Profit", "Margin %")
            items.forEach {
                appendRow(
                    it.name,
                    it.quantity.toString(),
                    money(it.gross),
                    money(it.cost),
                    money(it.profit),
                    it.marginPercent?.let(::format1dp) ?: "",
                )
            }

            appendLine()
            appendRow("Category", "Qty", "Gross")
            categories.forEach { appendRow(it.name, it.quantity.toString(), money(it.gross)) }

            appendLine()
            appendRow("Audit", "Staff", "Detail", "Reason", "Amount")
            audit.forEach { appendRow(it.kind.name, it.staffName, it.description, it.reason ?: "", money(it.amount)) }
        }
    }

    private val MUST_QUOTE = charArrayOf(',', '"', '\n', '\r')

    /** RFC 4180: quote when the value contains a comma, quote or newline; double inner quotes. */
    internal fun escape(value: String): String =
        if (value.any { it in MUST_QUOTE }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun StringBuilder.appendRow(vararg cells: String) {
        append(cells.joinToString(",") { escape(it) })
        append('\n')
    }

    private fun majorUnits(money: Money, minorUnits: Int): String =
        BigDecimal.valueOf(money.minor).movePointLeft(minorUnits).toPlainString()

    private fun format1dp(value: Double): String =
        BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString()
}
