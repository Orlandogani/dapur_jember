package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.data.database.Fixtures
import com.leanecorps.dapurjember.core.data.database.RoomDbTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemModifierGroupDaoTest : RoomDbTest() {

    private val dao by lazy { db.itemModifierGroupDao() }

    private suspend fun seed() {
        db.categoryDao().upsert(Fixtures.category())
        db.menuItemDao().upsert(Fixtures.menuItem())
        db.modifierGroupDao().upsert(Fixtures.modifierGroup())
    }

    @Test
    fun `links an item to a group`() = runTest {
        seed()
        dao.insert(Fixtures.itemModifierGroup(id = "l1"))

        assertEquals(listOf("l1"), dao.observeForItem("item-1").first().map { it.id })
    }

    @Test
    fun `the same item-group pair cannot be linked twice`() = runTest {
        seed()
        dao.insert(Fixtures.itemModifierGroup(id = "l1"))

        val failure = runCatching {
            dao.insert(Fixtures.itemModifierGroup(id = "l2"))
        }.exceptionOrNull()

        assertTrue("expected a unique-index violation, got $failure", failure is SQLiteConstraintException)
    }
}
