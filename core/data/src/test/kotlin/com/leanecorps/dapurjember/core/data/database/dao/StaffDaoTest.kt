package com.leanecorps.dapurjember.core.data.database.dao

import com.leanecorps.dapurjember.core.testing.database.OperationalEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StaffDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.staffDao() }

    @Test
    fun `observeActive returns only active, non-deleted staff`() = runTest {
        dao.upsert(OperationalEntityFixtures.staff(id = "a", name = "Ana", active = true))
        dao.upsert(OperationalEntityFixtures.staff(id = "b", name = "Budi", active = false))
        dao.upsert(OperationalEntityFixtures.staff(id = "c", name = "Cici", active = true, deletedAt = 1L))

        assertEquals(listOf("a"), dao.observeActive().first().map { it.id })
        assertEquals(listOf("a", "b"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun `softDelete hides the row from getById`() = runTest {
        dao.upsert(OperationalEntityFixtures.staff(id = "a"))
        dao.softDelete("a", deletedAt = 99L)

        assertEquals(null, dao.getById("a"))
    }
}
