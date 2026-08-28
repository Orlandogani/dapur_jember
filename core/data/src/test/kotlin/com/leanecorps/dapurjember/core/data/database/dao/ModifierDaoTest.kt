package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.data.database.Fixtures
import com.leanecorps.dapurjember.core.data.database.RoomDbTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModifierDaoTest : RoomDbTest() {

    private val groups by lazy { db.modifierGroupDao() }
    private val dao by lazy { db.modifierDao() }

    @Test
    fun `modifier requires an existing group`() = runTest {
        val failure = runCatching {
            dao.upsert(Fixtures.modifier(modifierGroupId = "ghost"))
        }.exceptionOrNull()

        assertTrue("expected a constraint violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `observeForGroup returns live modifiers ordered by sort_order`() = runTest {
        groups.upsert(Fixtures.modifierGroup(id = "grp-1"))
        dao.upsert(Fixtures.modifier(id = "hot", name = "Hot", sortOrder = 2))
        dao.upsert(Fixtures.modifier(id = "mild", name = "Mild", sortOrder = 1))
        dao.upsert(Fixtures.modifier(id = "removed", name = "Removed", sortOrder = 3, deletedAt = 7L))

        assertEquals(listOf("mild", "hot"), dao.observeForGroup("grp-1").first().map { it.id })
    }
}
