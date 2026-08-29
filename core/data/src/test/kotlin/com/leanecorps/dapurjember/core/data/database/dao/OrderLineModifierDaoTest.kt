package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.testing.database.OrderEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import com.leanecorps.dapurjember.core.testing.database.seedOrderPrerequisites
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderLineModifierDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.orderLineModifierDao() }

    private suspend fun seedLine() {
        db.seedOrderPrerequisites()
        db.orderDao().upsert(OrderEntityFixtures.order())
        db.orderLineDao().upsert(OrderEntityFixtures.orderLine(id = "line-1"))
    }

    @Test
    fun `a chosen modifier requires an existing line`() = runTest {
        db.seedOrderPrerequisites()
        val failure = runCatching {
            dao.insert(OrderEntityFixtures.orderLineModifier(orderLineId = "ghost"))
        }.exceptionOrNull()

        assertTrue("expected a constraint violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `observeForLine returns the live modifiers for that line`() = runTest {
        seedLine()
        dao.insertAll(
            listOf(
                OrderEntityFixtures.orderLineModifier(id = "m1", modifierId = "mod-1"),
            ),
        )

        assertEquals(listOf("m1"), dao.observeForLine("line-1").first().map { it.id })
    }
}
