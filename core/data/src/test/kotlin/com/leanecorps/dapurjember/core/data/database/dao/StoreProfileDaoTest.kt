package com.leanecorps.dapurjember.core.data.database.dao

import app.cash.turbine.test
import com.leanecorps.dapurjember.core.testing.database.MenuEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoreProfileDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.storeProfileDao() }

    @Test
    fun `observe emits null before setup and the profile after`() = runTest {
        dao.observe().test {
            assertNull(awaitItem())
            dao.upsert(MenuEntityFixtures.storeProfile(name = "Dapur Jember"))
            assertEquals("Dapur Jember", awaitItem()?.name)
        }
    }

    @Test
    fun `upsert replaces the single row`() = runTest {
        dao.upsert(MenuEntityFixtures.storeProfile(id = "store-1", name = "First"))
        dao.upsert(MenuEntityFixtures.storeProfile(id = "store-1", name = "Renamed"))

        assertEquals("Renamed", dao.get()?.name)
    }
}
