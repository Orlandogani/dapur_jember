package com.leanecorps.dapurjember.core.data.database.dao

import android.database.sqlite.SQLiteConstraintException
import com.leanecorps.dapurjember.core.testing.database.InventoryEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.MenuEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.OperationalEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryDaoTest : RoomDatabaseTest() {

    @Test
    fun `observeLowStock returns ingredients at or below their threshold`() = runTest {
        val dao = db.ingredientDao()
        fun ing(id: String, stock: Double) =
            InventoryEntityFixtures.ingredient(id = id, currentStockBase = stock, lowStockThresholdBase = 1_000.0)
        dao.upsert(ing("low", 500.0))
        dao.upsert(ing("ok", 5_000.0))
        dao.upsert(ing("exact", 1_000.0))

        assertEquals(setOf("low", "exact"), dao.observeLowStock().first().map { it.id }.toSet())
    }

    @Test
    fun `updateStock writes the new level and average cost`() = runTest {
        val dao = db.ingredientDao()
        dao.upsert(InventoryEntityFixtures.ingredient(id = "ing-1", currentStockBase = 1_000.0))

        dao.updateStock("ing-1", stockBase = 850.5, avgCostMinor = 7, updatedAt = 100L)

        val row = dao.getById("ing-1")!!
        assertEquals(850.5, row.currentStockBase, 0.0)
        assertEquals(7L, row.avgCostPerBaseMinor)
        assertEquals(2, row.revision)
    }

    @Test
    fun `an ingredient with an unknown supplier is rejected`() = runTest {
        val failure = runCatching {
            db.ingredientDao().upsert(InventoryEntityFixtures.ingredient(supplierId = "ghost"))
        }.exceptionOrNull()
        assertTrue("expected a constraint violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `a recipe line links a variant to an ingredient and cannot be added twice`() = runTest {
        db.categoryDao().upsert(MenuEntityFixtures.category())
        db.menuItemDao().upsert(MenuEntityFixtures.menuItem())
        db.menuVariantDao().upsert(MenuEntityFixtures.menuVariant())
        db.ingredientDao().upsert(InventoryEntityFixtures.ingredient())
        val dao = db.recipeLineDao()

        dao.insert(InventoryEntityFixtures.recipeLine(id = "rl-1"))
        assertEquals(listOf("rl-1"), dao.getForVariant("var-1").map { it.id })

        val failure = runCatching {
            dao.insert(InventoryEntityFixtures.recipeLine(id = "rl-2"))
        }.exceptionOrNull()
        assertTrue("expected a unique-index violation, got $failure", failure is SQLiteConstraintException)
    }

    @Test
    fun `stock movements are listed newest first for an ingredient`() = runTest {
        db.staffDao().upsert(OperationalEntityFixtures.staff())
        db.ingredientDao().upsert(InventoryEntityFixtures.ingredient())
        val dao = db.stockMovementDao()

        dao.insert(InventoryEntityFixtures.stockMovement(id = "old", createdAt = 10L, reason = "OPENING"))
        dao.insert(InventoryEntityFixtures.stockMovement(id = "new", createdAt = 20L, reason = "SALE"))

        assertEquals(listOf("new", "old"), dao.observeForIngredient("ing-1").first().map { it.id })
    }
}
