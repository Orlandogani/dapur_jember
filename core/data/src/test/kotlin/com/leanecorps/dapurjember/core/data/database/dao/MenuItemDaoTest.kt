package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.data.database.Fixtures
import com.leanecorps.dapurjember.core.data.database.RoomDbTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuItemDaoTest : RoomDbTest() {

    private val dao by lazy { db.menuItemDao() }

    @Test
    fun `insert with unknown category is rejected by the foreign key`() = runTest {
        val failure = runCatching {
            dao.upsert(Fixtures.menuItem(categoryId = "does-not-exist"))
        }.exceptionOrNull()

        assertTrue("expected a constraint violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `observeByCategory returns only live rows for that category`() = runTest {
        db.categoryDao().upsert(Fixtures.category(id = "food"))
        db.categoryDao().upsert(Fixtures.category(id = "drink"))
        dao.upsert(Fixtures.menuItem(id = "a", categoryId = "food", sortOrder = 1))
        dao.upsert(Fixtures.menuItem(id = "b", categoryId = "food", sortOrder = 2, deletedAt = 1L))
        dao.upsert(Fixtures.menuItem(id = "c", categoryId = "drink"))

        assertEquals(listOf("a"), dao.observeByCategory("food").first().map { it.id })
    }
}
