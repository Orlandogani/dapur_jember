package com.leanecorps.dapurjember.core.data.database.dao

import com.leanecorps.dapurjember.core.testing.database.MenuEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChangeLogDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.changeLogDao() }

    @Test
    fun `observeUnsynced returns only rows without synced_at, oldest first`() = runTest {
        dao.insert(MenuEntityFixtures.changeLog(id = "b", timestamp = 20L))
        dao.insert(MenuEntityFixtures.changeLog(id = "a", timestamp = 10L))
        dao.insert(MenuEntityFixtures.changeLog(id = "done", timestamp = 5L, syncedAt = 99L))

        assertEquals(listOf("a", "b"), dao.observeUnsynced().first().map { it.id })
        assertEquals(2, dao.unsyncedCount())
    }

    @Test
    fun `markSynced clears rows from the unsynced view`() = runTest {
        dao.insert(MenuEntityFixtures.changeLog(id = "a", timestamp = 10L))
        dao.insert(MenuEntityFixtures.changeLog(id = "b", timestamp = 20L))

        dao.markSynced(listOf("a"), syncedAt = 1_000L)

        assertEquals(listOf("b"), dao.observeUnsynced().first().map { it.id })
    }
}
