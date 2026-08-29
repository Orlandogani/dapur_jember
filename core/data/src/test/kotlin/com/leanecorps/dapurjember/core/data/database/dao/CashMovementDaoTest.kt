package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.testing.database.OperationalEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CashMovementDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.cashMovementDao() }

    private suspend fun seedShift() {
        db.staffDao().upsert(OperationalEntityFixtures.staff())
        db.shiftDao().upsert(OperationalEntityFixtures.shift())
    }

    @Test
    fun `a movement requires an existing shift`() = runTest {
        db.staffDao().upsert(OperationalEntityFixtures.staff())
        val failure = runCatching {
            dao.insert(OperationalEntityFixtures.cashMovement(shiftId = "ghost"))
        }.exceptionOrNull()

        assertTrue("expected a constraint violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `observeForShift returns live movements oldest first`() = runTest {
        seedShift()
        dao.insert(OperationalEntityFixtures.cashMovement(id = "b", createdAt = 20L))
        dao.insert(OperationalEntityFixtures.cashMovement(id = "a", createdAt = 10L))

        assertEquals(listOf("a", "b"), dao.observeForShift("shift-1").first().map { it.id })

        dao.softDelete("a", deletedAt = 30L)
        assertEquals(listOf("b"), dao.observeForShift("shift-1").first().map { it.id })
    }
}
