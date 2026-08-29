package com.leanecorps.dapurjember.core.data.database.dao

import com.leanecorps.dapurjember.core.testing.database.OrderEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import com.leanecorps.dapurjember.core.testing.database.seedOrderPrerequisites
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderLineDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.orderLineDao() }

    private suspend fun seedOrder() {
        db.seedOrderPrerequisites()
        db.orderDao().upsert(OrderEntityFixtures.order(id = "order-1"))
    }

    @Test
    fun `getUnsent returns only active, unsent, live lines - ordered by course`() = runTest {
        seedOrder()
        dao.upsert(OrderEntityFixtures.orderLine(id = "main", course = 2))
        dao.upsert(OrderEntityFixtures.orderLine(id = "starter", course = 1))
        dao.upsert(OrderEntityFixtures.orderLine(id = "already-sent", course = 1, sentAt = 100L))
        dao.upsert(OrderEntityFixtures.orderLine(id = "voided", course = 1, state = "VOIDED"))
        dao.upsert(OrderEntityFixtures.orderLine(id = "deleted", course = 1, deletedAt = 5L))

        assertEquals(listOf("starter", "main"), dao.getUnsent("order-1").map { it.id })
    }

    @Test
    fun `markSent stamps sent_at and removes the lines from getUnsent`() = runTest {
        seedOrder()
        dao.upsert(OrderEntityFixtures.orderLine(id = "l1"))
        dao.upsert(OrderEntityFixtures.orderLine(id = "l2"))

        dao.markSent(listOf("l1"), sentAt = 9_000L)

        assertEquals(listOf("l2"), dao.getUnsent("order-1").map { it.id })
        assertEquals(9_000L, dao.observeForOrder("order-1").first().first { it.id == "l1" }.sentAt)
    }

    @Test
    fun `voidLine marks the line VOIDED with a reason but keeps it visible`() = runTest {
        seedOrder()
        dao.upsert(OrderEntityFixtures.orderLine(id = "l1"))

        dao.voidLine("l1", reason = "customer changed mind", updatedAt = 7_000L)

        val line = dao.observeForOrder("order-1").first().single()
        assertEquals("VOIDED", line.state)
        assertEquals("customer changed mind", line.voidReason)
    }
}
