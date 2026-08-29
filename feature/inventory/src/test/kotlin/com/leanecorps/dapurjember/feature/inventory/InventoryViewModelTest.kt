package com.leanecorps.dapurjember.feature.inventory

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.inventory.BaseUnit
import com.leanecorps.dapurjember.core.domain.inventory.Ingredient
import com.leanecorps.dapurjember.core.domain.inventory.StockReason
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeInventoryRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeSessionRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeStoreProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val inventory = FakeInventoryRepository()
    private val session = FakeSessionRepository() // currentStaffId() = "staff-1"
    private val profiles = FakeStoreProfileRepository()
    private val viewModel by lazy { InventoryViewModel(inventory, session, profiles) }

    @Test
    fun `saving a new ingredient adds it to the list`() = runTest {
        viewModel.startAdd()
        viewModel.editIngredient { it.copy(name = "Rice", purchaseUnit = "sack", purchaseToBaseFactorText = "25000") }
        viewModel.saveIngredient()
        advanceUntilIdle()

        assertEquals(listOf("Rice"), inventory.observeIngredients().first().map { it.name })
    }

    @Test
    fun `applying a purchase adjustment moves stock and cost`() = runTest {
        inventory.upsertIngredient(
            Ingredient(
                id = "i1",
                name = "Chicken",
                baseUnit = BaseUnit.G,
                purchaseUnit = "kg",
                purchaseToBaseFactor = 1_000.0,
                currentStockBase = 0.0,
                avgCostPerBase = Money.ZERO,
                lowStockThresholdBase = 500.0,
            ),
        )
        viewModel.startAdjust("i1")
        advanceUntilIdle()
        viewModel.editAdjust { it.copy(qtyText = "1000", reason = StockReason.PURCHASE, unitCostText = "50") }
        viewModel.applyAdjust()
        advanceUntilIdle()

        val chicken = inventory.getIngredient("i1")!!
        assertEquals(1_000.0, chicken.currentStockBase, 0.0)
        assertEquals(Money(50), chicken.avgCostPerBase)
    }
}
