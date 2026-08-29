package com.leanecorps.dapurjember.core.domain.reports

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.order.PaymentMethod

/**
 * Reports read from the same local DB as everything else (FR-R1 — offline). Every query
 * filters on the indexed `business_day` TEXT column, never a `paid_at` millis range
 * (`docs/3-data-model` §4).
 */
interface ReportsRepository {

    suspend fun dailySummary(businessDay: String): DailySummary

    /** Sales by menu item for the day, best-selling by revenue first (S23). */
    suspend fun salesByItem(businessDay: String): List<ItemSales>
}

data class DailySummary(
    val businessDay: String,
    val orderCount: Int,
    val covers: Int,
    val grossRevenue: Money,
    val paymentMix: List<PaymentMixRow>,
    val discountCount: Int,
    val discountTotal: Money,
    val voidedOrders: Int,
    val voidedLines: Int,
) {
    /** Gross revenue divided across paid orders; zero when nothing has been sold. */
    val averageTicket: Money
        get() = if (orderCount == 0) Money.ZERO else Money(grossRevenue.minor / orderCount)
}

data class PaymentMixRow(val method: PaymentMethod, val amount: Money)

data class ItemSales(val name: String, val quantity: Int, val gross: Money)
