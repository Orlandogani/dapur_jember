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

    /** Sales grouped by category, best-selling by revenue first (FR-R2). */
    suspend fun salesByCategory(businessDay: String): List<CategorySales>

    /**
     * Voids and discounts for the day, with who authorised each (S25). The trust feature —
     * it is what lets an owner see that staff cannot quietly delete a sale.
     */
    suspend fun auditEntries(businessDay: String): List<AuditEntry>
}

data class DailySummary(
    val businessDay: String,
    val orderCount: Int,
    val covers: Int,
    val grossRevenue: Money,
    /** Cost of goods sold — only counts items that have a recipe (FR-I4). */
    val cogs: Money,
    val paymentMix: List<PaymentMixRow>,
    val discountCount: Int,
    val discountTotal: Money,
    val voidedOrders: Int,
    val voidedLines: Int,
) {
    /** Gross revenue divided across paid orders; zero when nothing has been sold. */
    val averageTicket: Money
        get() = if (orderCount == 0) Money.ZERO else Money(grossRevenue.minor / orderCount)

    val grossProfit: Money get() = grossRevenue - cogs

    /**
     * Gross margin as a percentage of revenue, or `null` when there is no revenue to divide
     * by — a null renders as "—" rather than a misleading 0%.
     */
    val grossMarginPercent: Double?
        get() = if (grossRevenue.isZero) null else grossProfit.minor * PERCENT / grossRevenue.minor
}

data class PaymentMixRow(val method: PaymentMethod, val amount: Money)

data class ItemSales(
    val name: String,
    val quantity: Int,
    val gross: Money,
    /** Recipe cost of the quantity sold; zero for items with no recipe. */
    val cost: Money,
) {
    val profit: Money get() = gross - cost

    val marginPercent: Double?
        get() = if (gross.isZero) null else profit.minor * PERCENT / gross.minor
}

data class CategorySales(val name: String, val quantity: Int, val gross: Money)

enum class AuditKind { VOID, DISCOUNT }

/** One privileged action, resolved to names for display (S25). */
data class AuditEntry(
    val kind: AuditKind,
    val staffName: String,
    val description: String,
    val reason: String?,
    val amount: Money,
    val at: Long,
)

private const val PERCENT = 100.0
