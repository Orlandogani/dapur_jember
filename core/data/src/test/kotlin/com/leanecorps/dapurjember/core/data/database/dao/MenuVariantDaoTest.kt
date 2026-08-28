package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.data.database.Fixtures
import com.leanecorps.dapurjember.core.data.database.RoomDbTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuVariantDaoTest : RoomDbTest() {

    private val dao by lazy { db.menuVariantDao() }

    private suspend fun seedItem() {
        db.categoryDao().upsert(Fixtures.category())
        db.menuItemDao().upsert(Fixtures.menuItem())
    }

    @Test
    fun `variant requires an existing menu item`() = runTest {
        val failure = runCatching {
            dao.upsert(Fixtures.menuVariant(menuItemId = "ghost"))
        }.exceptionOrNull()

        assertTrue("expected a constraint violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `observeForItem returns live variants ordered by sort_order`() = runTest {
        seedItem()
        dao.upsert(Fixtures.menuVariant(id = "large", name = "Large", sortOrder = 2))
        dao.upsert(Fixtures.menuVariant(id = "small", name = "Small", sortOrder = 1))
        dao.upsert(Fixtures.menuVariant(id = "old", name = "Old", sortOrder = 3, deletedAt = 1L))

        assertEquals(listOf("small", "large"), dao.observeForItem("item-1").first().map { it.id })
    }
}
