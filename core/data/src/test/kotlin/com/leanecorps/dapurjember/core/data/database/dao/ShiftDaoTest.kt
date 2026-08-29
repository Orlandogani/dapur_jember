package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.testing.database.OperationalEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.shiftDao() }

    private suspend fun seedStaff(id: String = "staff-1") {
        db.staffDao().upsert(OperationalEntityFixtures.staff(id = id))
    }

    @Test
    fun `a shift requires an existing opener`() = runTest {
        val failure = runCatching {
            dao.upsert(OperationalEntityFixtures.shift(openedBy = "ghost"))
        }.exceptionOrNull()

        assertTrue("expected a constraint violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `observeOpenShift tracks the shift with no closed_at and clears when it closes`() = runTest {
        seedStaff()
        dao.upsert(OperationalEntityFixtures.shift(id = "s1", openedAt = 10L))

        assertEquals("s1", dao.observeOpenShift().first()?.id)

        dao.upsert(OperationalEntityFixtures.shift(id = "s1", openedAt = 10L, closedAt = 99L))

        assertNull(dao.observeOpenShift().first())
    }

    @Test
    fun `observeForBusinessDay filters by the reporting key`() = runTest {
        seedStaff()
        dao.upsert(OperationalEntityFixtures.shift(id = "mon", businessDay = "2026-08-24", openedAt = 1L))
        dao.upsert(OperationalEntityFixtures.shift(id = "tue", businessDay = "2026-08-25", openedAt = 2L))

        assertEquals(listOf("tue"), dao.observeForBusinessDay("2026-08-25").first().map { it.id })
    }
}
