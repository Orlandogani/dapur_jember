package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.testing.database.MenuEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuVariantDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.menuVariantDao() }

    private suspend fun seedItem() {
        db.categoryDao().upsert(MenuEntityFixtures.category())
        db.menuItemDao().upsert(MenuEntityFixtures.menuItem())
    }

    @Test
    fun `variant requires an existing menu item`() = runTest {
        val failure = runCatching {
            dao.upsert(MenuEntityFixtures.menuVariant(menuItemId = "ghost"))
        }.exceptionOrNull()

        assertTrue("expected a constraint violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `observeForItem returns live variants ordered by sort_order`() = runTest {
        seedItem()
        dao.upsert(MenuEntityFixtures.menuVariant(id = "large", name = "Large", sortOrder = 2))
        dao.upsert(MenuEntityFixtures.menuVariant(id = "small", name = "Small", sortOrder = 1))
        dao.upsert(MenuEntityFixtures.menuVariant(id = "old", name = "Old", sortOrder = 3, deletedAt = 1L))

        assertEquals(listOf("small", "large"), dao.observeForItem("item-1").first().map { it.id })
    }
}
