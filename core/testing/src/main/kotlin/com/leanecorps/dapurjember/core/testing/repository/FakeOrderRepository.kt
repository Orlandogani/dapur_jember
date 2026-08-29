package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.order.AddLineParams
import com.leanecorps.dapurjember.core.domain.order.ApplyDiscountParams
import com.leanecorps.dapurjember.core.domain.order.OpenOrderParams
import com.leanecorps.dapurjember.core.domain.order.Order
import com.leanecorps.dapurjember.core.domain.order.OrderEvent
import com.leanecorps.dapurjember.core.domain.order.OrderLine
import com.leanecorps.dapurjember.core.domain.order.OrderRepository
import com.leanecorps.dapurjember.core.domain.order.OrderState
import com.leanecorps.dapurjember.core.domain.order.OrderStateMachine
import com.leanecorps.dapurjember.core.domain.order.OrderTotals
import com.leanecorps.dapurjember.core.domain.order.Payment
import com.leanecorps.dapurjember.core.domain.order.RecordPaymentParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/**
 * A lightweight in-memory [OrderRepository] for ViewModel tests. Totals are a plain sum of
 * line effective-unit-price × qty (no tax / service charge / discount engine).
 */
class FakeOrderRepository : OrderRepository {

    private val orders = MutableStateFlow<Map<String, Order>>(emptyMap())
    private val ids = AtomicLong(0)

    private fun nextId(prefix: String) = "$prefix-${ids.incrementAndGet()}"

    fun order(id: String): Order? = orders.value[id]

    private fun recompute(order: Order): Order {
        val subtotal = order.lines.filterNot { it.voided }
            .fold(Money.ZERO) { acc, l -> acc + (l.effectiveUnitPrice * l.quantity) }
        return order.copy(totals = OrderTotals(subtotal = subtotal, total = subtotal))
    }

    private fun put(order: Order) = orders.update { it + (order.id to recompute(order)) }

    override fun observeOrder(orderId: String): Flow<Order?> = orders.map { it[orderId] }

    override fun observeActiveOrderForTable(tableId: String): Flow<Order?> =
        orders.map { map ->
            map.values.firstOrNull { it.diningTableId == tableId && !it.state.isTerminal }
        }

    override suspend fun getOrder(orderId: String): Order? = orders.value[orderId]

    override suspend fun openOrder(params: OpenOrderParams): String {
        val id = nextId("order")
        put(
            Order(
                id = id,
                orderNumber = params.orderNumber,
                diningTableId = params.diningTableId,
                shiftId = params.shiftId,
                openedByStaffId = params.openedByStaffId,
                state = OrderState.DRAFT,
                guestCount = params.guestCount,
                businessDay = params.businessDay,
                lines = emptyList(),
                payments = emptyList(),
                discounts = emptyList(),
                totals = OrderTotals(),
                openedAt = 0L,
                sentAt = null,
                paidAt = null,
                note = params.note,
            ),
        )
        return id
    }

    override suspend fun addLine(params: AddLineParams): String {
        val order = orders.value.getValue(params.orderId)
        val lineId = nextId("line")
        val line = OrderLine(
            id = lineId,
            menuVariantId = params.menuVariantId,
            itemName = params.menuVariantId,
            variantName = "Regular",
            unitPrice = Money(10_000),
            quantity = params.quantity,
            modifiers = emptyList(),
            note = params.note,
            course = params.course,
            sentAt = null,
            voided = false,
            voidReason = null,
        )
        put(order.copy(lines = order.lines + line))
        return lineId
    }

    override suspend fun setLineQuantity(lineId: String, quantity: Int) =
        mutateLine(lineId) { it.copy(quantity = quantity) }

    override suspend fun voidLine(lineId: String, reason: String, actorStaffId: String) =
        mutateLine(lineId) { it.copy(voided = true, voidReason = reason) }

    override suspend fun sendToKitchen(orderId: String): List<OrderLine> {
        val order = orders.value.getValue(orderId)
        val unsent = order.lines.filter { it.sentAt == null && !it.voided }
        put(
            order.copy(
                lines = order.lines.map { if (it.sentAt == null && !it.voided) it.copy(sentAt = 1L) else it },
                state = if (order.state == OrderState.DRAFT) OrderState.SENT else order.state,
            ),
        )
        return unsent
    }

    override suspend fun applyEvent(orderId: String, event: OrderEvent) {
        val order = orders.value.getValue(orderId)
        put(order.copy(state = OrderStateMachine.transition(order.state, event)))
    }

    override suspend fun applyDiscount(params: ApplyDiscountParams): String = nextId("discount")

    override suspend fun removeDiscount(discountId: String, actorStaffId: String) = Unit

    override suspend fun recordPayment(params: RecordPaymentParams): String {
        val order = orders.value.getValue(params.orderId)
        val id = nextId("payment")
        val amount = Money(params.amountMinor)
        val tendered = Money(params.tenderedMinor)
        put(
            order.copy(
                payments = order.payments + Payment(
                    id = id,
                    method = params.method,
                    amount = amount,
                    tendered = tendered,
                    change = (tendered - amount).let { if (it.isNegative) Money.ZERO else it },
                    reference = params.reference,
                ),
            ),
        )
        return id
    }

    override suspend fun isFullySettled(orderId: String): Boolean {
        val order = orders.value[orderId] ?: return false
        return order.amountPaid >= order.totals.total
    }

    var receiptReprints = 0
        private set

    override suspend fun reprintReceipt(orderId: String) {
        receiptReprints++
    }

    private inline fun mutateLine(lineId: String, transform: (OrderLine) -> OrderLine) {
        val order = orders.value.values.first { o -> o.lines.any { it.id == lineId } }
        put(order.copy(lines = order.lines.map { if (it.id == lineId) transform(it) else it }))
    }
}
