package com.leanecorps.dapurjember.core.data.database.dao

import com.leanecorps.dapurjember.core.testing.database.MenuEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ModifierGroupDaoTest : RoomDatabaseTest() {

    private val dao by lazy { db.modifierGroupDao() }

    @Test
    fun `upsert then getById round-trips`() = runTest {
        val group = MenuEntityFixtures.modifierGroup(id = "grp-1", name = "Sugar level")
        dao.upsert(group)

        assertEquals(group, dao.getById("grp-1"))
    }

    @Test
    fun `observeForItem resolves groups attached via the join table, ordered by link sort_order`() = runTest {
        db.categoryDao().upsert(MenuEntityFixtures.category())
        db.menuItemDao().upsert(MenuEntityFixtures.menuItem())
        dao.upsert(MenuEntityFixtures.modifierGroup(id = "spice", name = "Spice"))
        dao.upsert(MenuEntityFixtures.modifierGroup(id = "sugar", name = "Sugar"))
        dao.upsert(MenuEntityFixtures.modifierGroup(id = "hidden", name = "Hidden"))

        val links = db.itemModifierGroupDao()
        links.upsert(MenuEntityFixtures.itemModifierGroup(id = "l1", modifierGroupId = "sugar", sortOrder = 2))
        links.upsert(MenuEntityFixtures.itemModifierGroup(id = "l2", modifierGroupId = "spice", sortOrder = 1))
        links.upsert(
            MenuEntityFixtures.itemModifierGroup(id = "l3", modifierGroupId = "hidden", sortOrder = 3, deletedAt = 1L),
        )

        assertEquals(listOf("spice", "sugar"), dao.observeForItem("item-1").first().map { it.id })
    }
}
