package com.leanecorps.dapurjember.core.domain.order

import kotlinx.coroutines.flow.Flow

/**
 * The order aggregate. Every mutation runs in one transaction that also: recomputes and
 * stores the denormalised totals via the pricing engine, writes a `change_log` row, and —
 * for privileged actions (void, discount) — a `audit_log` row. State moves only through
 * [OrderStateMachine].
 */
interface OrderRepository {

    fun observeOrder(orderId: String): Flow<Order?>

    fun observeActiveOrderForTable(tableId: String): Flow<Order?>

    suspend fun getOrder(orderId: String): Order?

    /** Opens a new DRAFT order and returns its id. */
    suspend fun openOrder(params: OpenOrderParams): String

    /** Adds a line (snapshotting name + price + modifier deltas) and returns its id. */
    suspend fun addLine(params: AddLineParams): String

    suspend fun setLineQuantity(lineId: String, quantity: Int)

    /** Voids a line with a reason; writes `audit_log`. FR-O4. */
    suspend fun voidLine(lineId: String, reason: String, actorStaffId: String)

    /** Marks the order's unsent active lines as sent and returns exactly those (FR-O3). */
    suspend fun sendToKitchen(orderId: String): List<OrderLine>

    /** Applies an [OrderEvent]; throws [IllegalOrderTransitionException] if illegal. */
    suspend fun applyEvent(orderId: String, event: OrderEvent)

    /** Applies a whole-bill or per-line discount (FR-P4); writes `audit_log`. Returns its id. */
    suspend fun applyDiscount(params: ApplyDiscountParams): String

    suspend fun removeDiscount(discountId: String, actorStaffId: String)

    /** Records one payment (FR-P1..P3). Does not change order state. Returns its id. */
    suspend fun recordPayment(params: RecordPaymentParams): String

    /** True once the recorded payments cover the order total. */
    suspend fun isFullySettled(orderId: String): Boolean
}

data class ApplyDiscountParams(
    val orderId: String,
    val kind: DiscountKind,
    /** Basis points for PERCENT, minor units for FIXED. */
    val value: Long,
    val reason: String,
    val authorisedByStaffId: String,
    /** `null` = whole-bill discount. */
    val orderLineId: String? = null,
)

data class RecordPaymentParams(
    val orderId: String,
    val method: PaymentMethod,
    val amountMinor: Long,
    val tenderedMinor: Long,
    val staffId: String,
    val reference: String? = null,
)

data class OpenOrderParams(
    val orderNumber: String,
    val shiftId: String,
    val openedByStaffId: String,
    val businessDay: String,
    val diningTableId: String? = null,
    val guestCount: Int = 1,
    val note: String? = null,
)

data class AddLineParams(
    val orderId: String,
    val menuVariantId: String,
    val quantity: Int,
    val addedByStaffId: String,
    val course: Int = 1,
    val note: String? = null,
    val modifierIds: List<String> = emptyList(),
)
