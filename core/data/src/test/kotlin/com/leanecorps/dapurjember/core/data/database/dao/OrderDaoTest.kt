package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.testing.database.OperationalEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.OrderEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import com.leanecorps.dapurjember.core.testing.database.seedOrderPrerequisites
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.orderDao() }

    @Test
    fun `an order requires an existing shift`() = runTest {
        db.staffDao().upsert(OperationalEntityFixtures.staff())
        val failure = runCatching {
            dao.upsert(OrderEntityFixtures.order(shiftId = "ghost", diningTableId = null))
        }.exceptionOrNull()

        assertTrue("expected a constraint violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `upsert round-trips and observeById reacts to state changes`() = runTest {
        db.seedOrderPrerequisites()
        dao.upsert(OrderEntityFixtures.order(id = "o1", state = "DRAFT"))

        assertEquals("DRAFT", dao.getById("o1")?.state)

        dao.updateState("o1", state = "SENT", updatedAt = 5_000L)
        val updated = dao.observeById("o1").first()!!
        assertEquals("SENT", updated.state)
        assertEquals(2, updated.revision)
    }

    @Test
    fun `observeActiveForTable ignores closed and voided orders`() = runTest {
        db.seedOrderPrerequisites()
        dao.upsert(OrderEntityFixtures.order(id = "open", state = "SENT", openedAt = 20L))
        dao.upsert(OrderEntityFixtures.order(id = "done", state = "CLOSED", openedAt = 10L))

        assertEquals("open", dao.observeActiveForTable("table-1").first()?.id)

        dao.updateState("open", state = "VOIDED", updatedAt = 30L)
        assertNull(dao.observeActiveForTable("table-1").first())
    }
}
