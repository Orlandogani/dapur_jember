package com.leanecorps.dapurjember.core.domain.order

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class OrderStateMachineTest {

    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @MethodSource("legalRows")
    fun `legal transitions produce the expected state`(from: OrderState, event: OrderEvent, expected: OrderState) {
        assertEquals(expected, OrderStateMachine.transition(from, event))
        assertTrue(OrderStateMachine.canApply(from, event))
    }

    @ParameterizedTest(name = "{0} rejects {1}")
    @MethodSource("illegalPairs")
    fun `illegal transitions throw and are not applicable`(from: OrderState, event: OrderEvent) {
        assertFalse(OrderStateMachine.canApply(from, event))
        val exception = assertThrows(IllegalOrderTransitionException::class.java) {
            OrderStateMachine.transition(from, event)
        }
        assertEquals(from, exception.from)
        assertEquals(event, exception.event)
    }

    @Test
    fun `terminal states reject every event`() {
        for (terminal in listOf(OrderState.CLOSED, OrderState.VOIDED)) {
            assertTrue(terminal.isTerminal)
            assertEquals(emptySet<OrderEvent>(), OrderStateMachine.allowedEvents(terminal))
            for (event in OrderEvent.entries) {
                assertThrows(IllegalOrderTransitionException::class.java) {
                    OrderStateMachine.transition(terminal, event)
                }
            }
        }
    }

    @Test
    fun `a paid order cannot be voided - refunds are a separate flow`() {
        assertFalse(OrderStateMachine.canApply(OrderState.PAID, OrderEvent.VOID))
        assertThrows(IllegalOrderTransitionException::class.java) {
            OrderStateMachine.transition(OrderState.PAID, OrderEvent.VOID)
        }
    }

    @Test
    fun `a closed order cannot be sent`() {
        assertThrows(IllegalOrderTransitionException::class.java) {
            OrderStateMachine.transition(OrderState.CLOSED, OrderEvent.SEND)
        }
    }

    @Test
    fun `allowedEvents matches the transition table`() {
        assertEquals(setOf(OrderEvent.SEND, OrderEvent.VOID), OrderStateMachine.allowedEvents(OrderState.DRAFT))
        assertEquals(
            setOf(OrderEvent.SERVE_PARTIAL, OrderEvent.SERVE, OrderEvent.VOID),
            OrderStateMachine.allowedEvents(OrderState.SENT),
        )
        assertEquals(setOf(OrderEvent.CLOSE), OrderStateMachine.allowedEvents(OrderState.PAID))
    }

    @Test
    fun `isTerminal is true only for CLOSED and VOIDED`() {
        assertEquals(
            setOf(OrderState.CLOSED, OrderState.VOIDED),
            OrderState.entries.filter { it.isTerminal }.toSet(),
        )
    }

    @Test
    fun `storage value round-trips for every state and rejects junk`() {
        for (state in OrderState.entries) {
            assertEquals(state, OrderState.fromStorage(state.storageValue))
        }
        assertThrows(IllegalArgumentException::class.java) { OrderState.fromStorage("NOT_A_STATE") }
    }

    @Test
    fun `the happy path runs from DRAFT to CLOSED`() {
        var state = OrderState.DRAFT
        for (event in listOf(OrderEvent.SEND, OrderEvent.SERVE, OrderEvent.PAY, OrderEvent.CLOSE)) {
            state = OrderStateMachine.transition(state, event)
        }
        assertEquals(OrderState.CLOSED, state)
    }

    companion object {
        /** The 10 legal transitions, from `docs/2-architecture` 5.1. */
        private val legalTransitions = listOf(
            Triple(OrderState.DRAFT, OrderEvent.SEND, OrderState.SENT),
            Triple(OrderState.DRAFT, OrderEvent.VOID, OrderState.VOIDED),
            Triple(OrderState.SENT, OrderEvent.SERVE_PARTIAL, OrderState.PARTIALLY_SERVED),
            Triple(OrderState.SENT, OrderEvent.SERVE, OrderState.SERVED),
            Triple(OrderState.SENT, OrderEvent.VOID, OrderState.VOIDED),
            Triple(OrderState.PARTIALLY_SERVED, OrderEvent.SERVE, OrderState.SERVED),
            Triple(OrderState.PARTIALLY_SERVED, OrderEvent.VOID, OrderState.VOIDED),
            Triple(OrderState.SERVED, OrderEvent.PAY, OrderState.PAID),
            Triple(OrderState.SERVED, OrderEvent.VOID, OrderState.VOIDED),
            Triple(OrderState.PAID, OrderEvent.CLOSE, OrderState.CLOSED),
        )

        @JvmStatic
        fun legalRows(): List<Arguments> =
            legalTransitions.map { (from, event, to) -> Arguments.of(from, event, to) }

        @JvmStatic
        fun illegalPairs(): List<Arguments> {
            val legal = legalTransitions.map { it.first to it.second }.toSet()
            return OrderState.entries.flatMap { from ->
                OrderEvent.entries.mapNotNull { event ->
                    if ((from to event) in legal) null else Arguments.of(from, event)
                }
            }
        }
    }
}
