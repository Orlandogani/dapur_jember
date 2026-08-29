package com.leanecorps.dapurjember.core.printing.template

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.common.money.formatAmount
import com.leanecorps.dapurjember.core.printing.PaperWidth
import com.leanecorps.dapurjember.core.printing.escpos.Alignment
import com.leanecorps.dapurjember.core.printing.escpos.EscPosBuilder

/**
 * Input for a customer receipt. All money is minor units; the template formats with
 * [currencyMinorUnits] and shows [currencyCode] once. Caller supplies the denormalised
 * order totals verbatim (architecture §5.2 — a historical receipt must reproduce byte for
 * byte, so never recompute here).
 */
data class ReceiptData(
    val headerLines: List<String>,
    val orderNumber: String,
    val businessDay: String,
    val tableLabel: String?,
    val printedAt: String,
    val serverName: String,
    val lines: List<ReceiptItemLine>,
    val subtotalMinor: Long,
    val discountMinor: Long,
    val serviceChargeMinor: Long,
    val taxMinor: Long,
    val roundingMinor: Long,
    val totalMinor: Long,
    val payments: List<ReceiptPaymentLine>,
    val changeMinor: Long,
    val currencyCode: String,
    val currencyMinorUnits: Int,
    val footerLines: List<String>,
    val reprint: Boolean = false,
)

data class ReceiptItemLine(
    val quantity: Int,
    val name: String,
    val lineTotalMinor: Long,
    val modifiers: List<ReceiptModifierLine> = emptyList(),
)

data class ReceiptModifierLine(val name: String, val priceDeltaMinor: Long)

data class ReceiptPaymentLine(val method: String, val amountMinor: Long)

fun renderCustomerReceipt(data: ReceiptData, width: PaperWidth): ByteArray {
    val b = EscPosBuilder(width.columns)
    val money = { minor: Long -> formatAmount(Money(minor), data.currencyMinorUnits) }

    b.initialize()
    b.align(Alignment.CENTER)
    b.bold(true)
    data.headerLines.firstOrNull()?.let { b.line(it) }
    b.bold(false)
    data.headerLines.drop(1).forEach { b.line(it) }
    if (data.reprint) b.line("*** REPRINT ***")
    b.align(Alignment.LEFT)
    b.divider()

    b.row("Receipt", data.orderNumber)
    b.row("Date", data.printedAt)
    b.row("Day", data.businessDay)
    data.tableLabel?.let { b.row("Table", it) }
    b.row("Server", data.serverName)
    b.divider()

    renderItems(b, data.lines, money)
    b.divider()

    renderTotals(b, data, money)

    data.payments.forEach { b.row(it.method, money(it.amountMinor)) }
    if (data.changeMinor != 0L) b.row("Change", money(data.changeMinor))

    b.divider()
    b.align(Alignment.CENTER)
    data.footerLines.forEach { b.line(it) }
    b.align(Alignment.LEFT)
    b.feed(1)
    b.cut()
    return b.build()
}

private fun renderItems(b: EscPosBuilder, lines: List<ReceiptItemLine>, money: (Long) -> String) {
    lines.forEach { line ->
        b.row("${line.quantity} x ${line.name}", money(line.lineTotalMinor))
        line.modifiers.forEach { mod ->
            val delta = if (mod.priceDeltaMinor == 0L) "" else money(mod.priceDeltaMinor)
            b.row("   + ${mod.name}", delta)
        }
    }
}

private fun renderTotals(b: EscPosBuilder, data: ReceiptData, money: (Long) -> String) {
    b.row("Subtotal", money(data.subtotalMinor))
    if (data.discountMinor != 0L) b.row("Discount", money(-data.discountMinor))
    if (data.serviceChargeMinor != 0L) b.row("Service charge", money(data.serviceChargeMinor))
    if (data.taxMinor != 0L) b.row("Tax", money(data.taxMinor))
    if (data.roundingMinor != 0L) b.row("Rounding", money(data.roundingMinor))

    b.bold(true)
    b.size(1, 2)
    b.row("TOTAL", "${data.currencyCode} ${money(data.totalMinor)}")
    b.normalSize()
    b.bold(false)
    b.divider()
}
