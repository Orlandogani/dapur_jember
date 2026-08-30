package com.leanecorps.dapurjember.core.domain.order

import com.leanecorps.dapurjember.core.common.money.Money

/**
 * An order as the app works with it — the persisted `orders` row plus its lines. The
 * [totals] mirror the denormalised snapshot columns; they are recomputed by the pricing
 * engine on every mutation while the order is open.
 */
data class Order(
    val id: String,
    val orderNumber: String,
    val diningTableId: String?,
    val shiftId: String,
    val openedByStaffId: String,
    val state: OrderState,
    val guestCount: Int,
    val businessDay: String,
    val lines: List<OrderLine>,
    val payments: List<Payment>,
    val discounts: List<OrderDiscount>,
    val totals: OrderTotals,
    val openedAt: Long?,
    val sentAt: Long?,
    val paidAt: Long?,
    val note: String?,
) {
    val amountPaid: Money get() = payments.fold(Money.ZERO) { acc, p -> acc + p.amount }

    /** What is still owed; `<= 0` means the bill is settled. */
    val balanceDue: Money get() = totals.total - amountPaid
}

enum class PaymentMethod { CASH, CARD, EWALLET, OTHER }

/**
 * The fixed reason list a void must pick from (FR-O4). Free text may be appended, but a
 * reason is never optional — a void with no stated cause is indistinguishable from theft.
 */
enum class VoidReason {
    WRONG_ORDER,
    CUSTOMER_CHANGED_MIND,
    QUALITY_ISSUE,
    OUT_OF_STOCK,
    STAFF_ERROR,
    OTHER,
    ;

    val label: String get() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}

data class Payment(
    val id: String,
    val method: PaymentMethod,
    val amount: Money,
    val tendered: Money,
    val change: Money,
    val reference: String?,
)

enum class DiscountKind { PERCENT, FIXED }

data class OrderDiscount(
    val id: String,
    /** `null` = whole-bill discount. */
    val orderLineId: String?,
    val kind: DiscountKind,
    /** Basis points when [kind] is PERCENT, minor units when FIXED. */
    val value: Long,
    val computed: Money,
    val reason: String,
)

data class OrderLine(
    val id: String,
    val menuVariantId: String,
    val itemName: String,
    val variantName: String,
    val unitPrice: Money,
    val quantity: Int,
    val modifiers: List<OrderLineModifier>,
    val note: String?,
    val course: Int,
    /** `null` until the line has been printed to the kitchen (FR-O3). */
    val sentAt: Long?,
    val voided: Boolean,
    val voidReason: String?,
) {
    /** Unit price with modifier deltas folded in. */
    val effectiveUnitPrice: Money get() = modifiers.fold(unitPrice) { acc, m -> acc + m.priceDelta }
}

data class OrderLineModifier(
    val id: String,
    val modifierId: String,
    val name: String,
    val priceDelta: Money,
)

/** Mirrors `orders.{subtotal,discount,service_charge,tax,rounding,total}_minor`. */
data class OrderTotals(
    val subtotal: Money = Money.ZERO,
    val discount: Money = Money.ZERO,
    val serviceCharge: Money = Money.ZERO,
    val tax: Money = Money.ZERO,
    val rounding: Money = Money.ZERO,
    val total: Money = Money.ZERO,
)
