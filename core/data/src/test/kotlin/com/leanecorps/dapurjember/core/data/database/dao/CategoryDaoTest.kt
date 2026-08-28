package com.leanecorps.dapurjember.core.data.database.dao

import app.cash.turbine.test
import com.leanecorps.dapurjember.core.testing.database.MenuEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.categoryDao() }

    @Test
    fun `upsert then observe round-trips the row`() = runTest {
        val category = MenuEntityFixtures.category(id = "cat-1", name = "Drinks")
        dao.upsert(category)

        assertEquals(category, dao.getById("cat-1"))
        assertEquals(listOf(category), dao.observeAll().first())
    }

    @Test
    fun `observeAll excludes soft-deleted rows`() = runTest {
        dao.upsert(MenuEntityFixtures.category(id = "live"))
        dao.upsert(MenuEntityFixtures.category(id = "gone", deletedAt = 999L))

        assertEquals(listOf("live"), dao.observeAll().first().map { it.id })
        assertNull(dao.getById("gone"))
    }

    @Test
    fun `softDelete removes the row from observeAll and stamps the envelope`() = runTest {
        dao.upsert(MenuEntityFixtures.category(id = "cat-1"))

        dao.observeAll().test {
            assertEquals(1, awaitItem().size)
            dao.softDelete("cat-1", deletedAt = 5_000L)
            assertEquals(emptyList<String>(), awaitItem().map { it.id })
        }
    }

    @Test
    fun `observeAll is ordered by sort_order`() = runTest {
        dao.upsert(MenuEntityFixtures.category(id = "b", name = "B", sortOrder = 2))
        dao.upsert(MenuEntityFixtures.category(id = "a", name = "A", sortOrder = 1))
        dao.upsert(MenuEntityFixtures.category(id = "c", name = "C", sortOrder = 3))

        assertEquals(listOf("a", "b", "c"), dao.observeAll().first().map { it.id })
    }
}
