package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.testing.database.MenuEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemModifierGroupDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.itemModifierGroupDao() }

    private suspend fun seed() {
        db.categoryDao().upsert(MenuEntityFixtures.category())
        db.menuItemDao().upsert(MenuEntityFixtures.menuItem())
        db.modifierGroupDao().upsert(MenuEntityFixtures.modifierGroup())
    }

    @Test
    fun `links an item to a group`() = runTest {
        seed()
        dao.insert(MenuEntityFixtures.itemModifierGroup(id = "l1"))

        assertEquals(listOf("l1"), dao.observeForItem("item-1").first().map { it.id })
    }

    @Test
    fun `the same item-group pair cannot be linked twice`() = runTest {
        seed()
        dao.insert(MenuEntityFixtures.itemModifierGroup(id = "l1"))

        val failure = runCatching {
            dao.insert(MenuEntityFixtures.itemModifierGroup(id = "l2"))
        }.exceptionOrNull()

        assertTrue("expected a unique-index violation, got $failure", failure is SQLiteConstraintException)
    }
}
