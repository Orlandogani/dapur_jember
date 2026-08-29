package com.leanecorps.dapurjember.core.domain.printing

/**
 * Everything a kitchen (or bar) ticket needs, with ids already resolved to names and the
 * timestamp already formatted in the store timezone. [lines] is pre-filtered to the lines
 * this ticket should show (FR-O3: only lines not previously sent).
 */
data class KitchenTicketData(
    val storeName: String,
    val stationTitle: String,
    val orderNumber: String,
    val tableLabel: String?,
    val orderType: String,
    val serverName: String,
    val printedAt: String,
    val lines: List<KitchenTicketLine>,
    val reprint: Boolean = false,
)

data class KitchenTicketLine(
    val quantity: Int,
    val name: String,
    val modifiers: List<String> = emptyList(),
    val note: String? = null,
    val course: Int = 1,
)

/**
 * Everything a customer receipt needs. All money is minor units. The caller supplies the
 * denormalised order totals verbatim (architecture §5.2 — a historical receipt must
 * reproduce byte for byte, so nothing is recomputed at print time).
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
