package com.leanecorps.dapurjember.core.data.order

import com.leanecorps.dapurjember.core.data.database.dao.OrderDao
import com.leanecorps.dapurjember.core.domain.order.OrderNumberGenerator
import javax.inject.Inject

/**
 * `NNN` order numbers that reset each business day (`orders.order_number`). Single-device, so
 * a plain count-then-increment is fine — no cross-terminal contention in v1.
 */
internal class SequentialOrderNumberGenerator @Inject constructor(
    private val orderDao: OrderDao,
) : OrderNumberGenerator {
    override suspend fun next(businessDay: String): String {
        val next = orderDao.countForBusinessDay(businessDay) + 1
        return next.toString().padStart(NUMBER_WIDTH, '0')
    }

    private companion object {
        const val NUMBER_WIDTH = 3
    }
}
