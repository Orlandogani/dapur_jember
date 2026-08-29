package com.leanecorps.dapurjember.core.data.database.dao

import com.leanecorps.dapurjember.core.testing.database.OrderEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import com.leanecorps.dapurjember.core.testing.database.seedOrderPrerequisites
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscountDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.discountDao() }

    private suspend fun seedOrderWithLine() {
        db.seedOrderPrerequisites()
        db.orderDao().upsert(OrderEntityFixtures.order(id = "order-1"))
        db.orderLineDao().upsert(OrderEntityFixtures.orderLine(id = "line-1"))
    }

    @Test
    fun `observeForOrder returns both bill-level and line-level discounts`() = runTest {
        seedOrderWithLine()
        dao.insert(OrderEntityFixtures.discount(id = "bill", orderLineId = null))
        dao.insert(OrderEntityFixtures.discount(id = "line", orderLineId = "line-1", type = "FIXED", value = 2_000))

        assertEquals(setOf("bill", "line"), dao.observeForOrder("order-1").first().map { it.id }.toSet())
        assertEquals(listOf("line"), dao.observeForLine("line-1").first().map { it.id })
    }

    @Test
    fun `softDelete hides a discount from the order view`() = runTest {
        seedOrderWithLine()
        dao.insert(OrderEntityFixtures.discount(id = "d1"))

        dao.softDelete("d1", deletedAt = 5L)

        assertEquals(emptyList<String>(), dao.observeForOrder("order-1").first().map { it.id })
    }
}
