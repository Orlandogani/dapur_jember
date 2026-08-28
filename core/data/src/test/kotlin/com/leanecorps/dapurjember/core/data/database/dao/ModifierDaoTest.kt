package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.testing.database.MenuEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModifierDaoTest : RoomDatabaseTest() {

    private val groups by lazy { db.modifierGroupDao() }
    private val dao by lazy { db.modifierDao() }

    @Test
    fun `modifier requires an existing group`() = runTest {
        val failure = runCatching {
            dao.upsert(MenuEntityFixtures.modifier(modifierGroupId = "ghost"))
        }.exceptionOrNull()

        assertTrue("expected a constraint violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `observeForGroup returns live modifiers ordered by sort_order`() = runTest {
        groups.upsert(MenuEntityFixtures.modifierGroup(id = "grp-1"))
        dao.upsert(MenuEntityFixtures.modifier(id = "hot", name = "Hot", sortOrder = 2))
        dao.upsert(MenuEntityFixtures.modifier(id = "mild", name = "Mild", sortOrder = 1))
        dao.upsert(MenuEntityFixtures.modifier(id = "removed", name = "Removed", sortOrder = 3, deletedAt = 7L))

        assertEquals(listOf("mild", "hot"), dao.observeForGroup("grp-1").first().map { it.id })
    }
}
