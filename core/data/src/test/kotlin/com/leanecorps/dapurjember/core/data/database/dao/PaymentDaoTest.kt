package com.leanecorps.dapurjember.core.data.database.dao

import com.leanecorps.dapurjember.core.testing.database.OrderEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import com.leanecorps.dapurjember.core.testing.database.seedOrderPrerequisites
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.paymentDao() }

    private suspend fun seedOrder() {
        db.seedOrderPrerequisites()
        db.orderDao().upsert(OrderEntityFixtures.order(id = "order-1"))
    }

    @Test
    fun `totalPaidMinor sums live payments across methods`() = runTest {
        seedOrder()
        dao.insert(OrderEntityFixtures.payment(id = "p1", method = "CASH", amountMinor = 20_000))
        dao.insert(OrderEntityFixtures.payment(id = "p2", method = "EWALLET", amountMinor = 15_000))

        assertEquals(35_000L, dao.totalPaidMinor("order-1"))

        dao.softDelete("p1", deletedAt = 1L)
        assertEquals(15_000L, dao.totalPaidMinor("order-1"))
    }

    @Test
    fun `totalPaidMinor is zero when nothing has been paid`() = runTest {
        seedOrder()
        assertEquals(0L, dao.totalPaidMinor("order-1"))
    }

    @Test
    fun `observeForOrder returns live payments oldest first`() = runTest {
        seedOrder()
        dao.insert(OrderEntityFixtures.payment(id = "b", createdAt = 20L))
        dao.insert(OrderEntityFixtures.payment(id = "a", createdAt = 10L))

        assertEquals(listOf("a", "b"), dao.observeForOrder("order-1").first().map { it.id })
    }
}
