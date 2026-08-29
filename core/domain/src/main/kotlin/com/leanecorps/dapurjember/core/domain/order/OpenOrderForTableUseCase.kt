package com.leanecorps.dapurjember.core.domain.order

import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** No shift is open, or no one is signed in. */
class NoActiveSessionException : IllegalStateException("no signed-in staff or open shift")

/**
 * Resumes the table's active order if it has one, otherwise opens a fresh DRAFT order for it.
 * Fills in the current session's staff / shift / business day and the next order number.
 */
class OpenOrderForTableUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val sessionRepository: SessionRepository,
    private val orderNumbers: OrderNumberGenerator,
) {
    suspend operator fun invoke(tableId: String, guestCount: Int): String {
        orderRepository.observeActiveOrderForTable(tableId).first()?.let { return it.id }

        val session = sessionRepository.currentSession() ?: throw NoActiveSessionException()
        return orderRepository.openOrder(
            OpenOrderParams(
                orderNumber = orderNumbers.next(session.businessDay),
                shiftId = session.shiftId,
                openedByStaffId = session.staffId,
                businessDay = session.businessDay,
                diningTableId = tableId,
                guestCount = guestCount,
            ),
        )
    }
}

/** Produces the human-readable order number that resets per business day (`orders.order_number`). */
interface OrderNumberGenerator {
    suspend fun next(businessDay: String): String
}
