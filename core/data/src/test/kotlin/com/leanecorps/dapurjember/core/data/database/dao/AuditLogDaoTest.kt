package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.testing.database.InventoryEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.OperationalEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditLogDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.auditLogDao() }

    @Test
    fun `an audit entry requires an existing actor`() = runTest {
        val failure = runCatching {
            dao.insert(InventoryEntityFixtures.auditLog(actorStaffId = "ghost"))
        }.exceptionOrNull()
        assertTrue("expected a constraint violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `observeRecent and observeForEntity return newest first`() = runTest {
        db.staffDao().upsert(OperationalEntityFixtures.staff())
        dao.insert(InventoryEntityFixtures.auditLog(id = "a", entityId = "line-1", createdAt = 10L))
        dao.insert(InventoryEntityFixtures.auditLog(id = "b", entityId = "line-1", createdAt = 20L))
        dao.insert(InventoryEntityFixtures.auditLog(id = "c", entityId = "line-9", createdAt = 30L))

        assertEquals(listOf("c", "b", "a"), dao.observeRecent(limit = 10).first().map { it.id })
        assertEquals(listOf("b", "a"), dao.observeForEntity("order_line", "line-1").first().map { it.id })
    }
}
