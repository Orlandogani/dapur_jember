package com.leanecorps.dapurjember.core.domain.order

import com.leanecorps.dapurjember.core.domain.order.OrderEvent.CLOSE
import com.leanecorps.dapurjember.core.domain.order.OrderEvent.PAY
import com.leanecorps.dapurjember.core.domain.order.OrderEvent.SEND
import com.leanecorps.dapurjember.core.domain.order.OrderEvent.SERVE
import com.leanecorps.dapurjember.core.domain.order.OrderEvent.SERVE_PARTIAL
import com.leanecorps.dapurjember.core.domain.order.OrderEvent.VOID
import com.leanecorps.dapurjember.core.domain.order.OrderState.CLOSED
import com.leanecorps.dapurjember.core.domain.order.OrderState.DRAFT
import com.leanecorps.dapurjember.core.domain.order.OrderState.PAID
import com.leanecorps.dapurjember.core.domain.order.OrderState.PARTIALLY_SERVED
import com.leanecorps.dapurjember.core.domain.order.OrderState.SENT
import com.leanecorps.dapurjember.core.domain.order.OrderState.SERVED
import com.leanecorps.dapurjember.core.domain.order.OrderState.VOIDED

/** Thrown when an [OrderEvent] is not legal from the current [OrderState]. */
class IllegalOrderTransitionException(
    val from: OrderState,
    val event: OrderEvent,
) : IllegalStateException("cannot apply $event from $from")

/**
 * The single source of truth for how an order may move between states (§5.1, FR-O5).
 * Illegal moves throw; there is deliberately no path that pays a `VOIDED` order, sends a
 * `CLOSED` one, or voids a `PAID` one (that would be a refund — a separate flow).
 */
object OrderStateMachine {

    private val transitions: Map<OrderState, Map<OrderEvent, OrderState>> = mapOf(
        DRAFT to mapOf(SEND to SENT, VOID to VOIDED),
        SENT to mapOf(SERVE_PARTIAL to PARTIALLY_SERVED, SERVE to SERVED, VOID to VOIDED),
        PARTIALLY_SERVED to mapOf(SERVE to SERVED, VOID to VOIDED),
        SERVED to mapOf(PAY to PAID, VOID to VOIDED),
        PAID to mapOf(CLOSE to CLOSED),
        CLOSED to emptyMap(),
        VOIDED to emptyMap(),
    )

    init {
        val missing = OrderState.entries.filterNot { it in transitions }
        require(missing.isEmpty()) { "OrderStateMachine has no transition row for: $missing" }
    }

    /** The resulting state, or throws [IllegalOrderTransitionException]. */
    fun transition(from: OrderState, event: OrderEvent): OrderState =
        transitions.getValue(from)[event] ?: throw IllegalOrderTransitionException(from, event)

    fun canApply(from: OrderState, event: OrderEvent): Boolean =
        transitions.getValue(from).containsKey(event)

    /** Every event legal from [from]; empty for terminal states. */
    fun allowedEvents(from: OrderState): Set<OrderEvent> =
        transitions.getValue(from).keys
}
