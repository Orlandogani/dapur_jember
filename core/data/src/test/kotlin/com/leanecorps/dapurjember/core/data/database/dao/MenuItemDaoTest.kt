package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.testing.database.MenuEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuItemDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.menuItemDao() }

    @Test
    fun `insert with unknown category is rejected by the foreign key`() = runTest {
        val failure = runCatching {
            dao.upsert(MenuEntityFixtures.menuItem(categoryId = "does-not-exist"))
        }.exceptionOrNull()

        assertTrue("expected a constraint violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `observeByCategory returns only live rows for that category`() = runTest {
        db.categoryDao().upsert(MenuEntityFixtures.category(id = "food"))
        db.categoryDao().upsert(MenuEntityFixtures.category(id = "drink"))
        dao.upsert(MenuEntityFixtures.menuItem(id = "a", categoryId = "food", sortOrder = 1))
        dao.upsert(MenuEntityFixtures.menuItem(id = "b", categoryId = "food", sortOrder = 2, deletedAt = 1L))
        dao.upsert(MenuEntityFixtures.menuItem(id = "c", categoryId = "drink"))

        assertEquals(listOf("a"), dao.observeByCategory("food").first().map { it.id })
    }
}
