package com.leanecorps.dapurjember.core.domain.order

import javax.inject.Inject

/**
 * Advances a fully-paid order to `PAID`, walking through `SEND`/`SERVE` if it hasn't been
 * sent or served yet — settling the bill implies the food went out. No-op if the balance
 * is not yet covered.
 */
class SettleOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
) {
    private val forward = listOf(OrderEvent.SEND, OrderEvent.SERVE, OrderEvent.PAY)

    suspend operator fun invoke(orderId: String) {
        if (!orderRepository.isFullySettled(orderId)) return
        var state = orderRepository.getOrder(orderId)?.state
        while (state != null && state != OrderState.PAID) {
            val current = state
            val next = forward.firstOrNull { OrderStateMachine.canApply(current, it) } ?: break
            orderRepository.applyEvent(orderId, next)
            state = OrderStateMachine.transition(current, next)
        }
    }
}
