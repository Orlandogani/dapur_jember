package com.leanecorps.dapurjember.core.data.database.dao

import com.leanecorps.dapurjember.core.testing.database.OperationalEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FloorAreaDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.floorAreaDao() }

    @Test
    fun `upsert then observe round-trips, ordered by sort_order`() = runTest {
        dao.upsert(OperationalEntityFixtures.floorArea(id = "b", name = "Bar", sortOrder = 2))
        dao.upsert(OperationalEntityFixtures.floorArea(id = "a", name = "Atrium", sortOrder = 1))

        assertEquals(listOf("a", "b"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun `observeAll and getById exclude soft-deleted rows`() = runTest {
        dao.upsert(OperationalEntityFixtures.floorArea(id = "live"))
        dao.upsert(OperationalEntityFixtures.floorArea(id = "gone", deletedAt = 10L))

        assertEquals(listOf("live"), dao.observeAll().first().map { it.id })
        assertNull(dao.getById("gone"))
    }
}
