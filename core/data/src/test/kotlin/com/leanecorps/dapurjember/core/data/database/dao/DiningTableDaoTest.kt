package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.testing.database.OperationalEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiningTableDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.diningTableDao() }

    private suspend fun seedArea(id: String = "area-1") {
        db.floorAreaDao().upsert(OperationalEntityFixtures.floorArea(id = id))
    }

    @Test
    fun `a table requires an existing floor area`() = runTest {
        val failure = runCatching {
            dao.upsert(OperationalEntityFixtures.diningTable(floorAreaId = "ghost"))
        }.exceptionOrNull()

        assertTrue("expected a constraint violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `observeByArea returns only live tables for that area`() = runTest {
        seedArea("hall")
        seedArea("patio")
        dao.upsert(OperationalEntityFixtures.diningTable(id = "h1", floorAreaId = "hall", label = "H1"))
        dao.upsert(OperationalEntityFixtures.diningTable(id = "h2", floorAreaId = "hall", label = "H2", deletedAt = 1L))
        dao.upsert(OperationalEntityFixtures.diningTable(id = "p1", floorAreaId = "patio", label = "P1"))

        assertEquals(listOf("h1"), dao.observeByArea("hall").first().map { it.id })
    }

    @Test
    fun `updateState changes the state and bumps the envelope`() = runTest {
        seedArea()
        dao.upsert(OperationalEntityFixtures.diningTable(id = "t1", state = "FREE"))

        dao.updateState("t1", state = "OCCUPIED", updatedAt = 5_000L)

        val table = dao.getById("t1")!!
        assertEquals("OCCUPIED", table.state)
        assertEquals(5_000L, table.updatedAt)
        assertEquals(2, table.revision)
    }
}
