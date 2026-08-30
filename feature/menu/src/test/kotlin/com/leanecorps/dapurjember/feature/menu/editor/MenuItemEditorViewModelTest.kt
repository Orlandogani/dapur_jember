package com.leanecorps.dapurjember.feature.menu.editor

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.auth.AuthoriseUseCase
import com.leanecorps.dapurjember.core.domain.auth.StaffRole
import com.leanecorps.dapurjember.core.domain.config.StoreProfile
import com.leanecorps.dapurjember.core.domain.inventory.BaseUnit
import com.leanecorps.dapurjember.core.domain.inventory.Ingredient
import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.MenuVariant
import com.leanecorps.dapurjember.core.domain.pricing.RoundingRule
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeAuthRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeInventoryRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeMenuRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeSessionRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeStoreProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class MenuItemEditorViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val menu = FakeMenuRepository()
    private val profiles = FakeStoreProfileRepository(
        StoreProfile(
            id = "p1",
            name = "Test",
            currencyCode = "USD",
            currencyMinorUnits = 2,
            taxRateBasisPoints = 0,
            taxInclusive = false,
            serviceChargeBasisPoints = 0,
            serviceChargeTaxable = false,
            roundingRule = RoundingRule.NONE,
            businessDayCutoffMinutes = 0,
            timezoneId = "UTC",
        ),
    )

    private val inventory = FakeInventoryRepository()

    private val session = FakeSessionRepository()
    private val auth = FakeAuthRepository(session)
    private val authorise = AuthoriseUseCase(auth, session)

    /** Signs someone in, so the MANAGE_MENU check has a role to look at. */
    private suspend fun signInAs(role: StaffRole) {
        auth.addStaff(id = "u1", name = "Staff", pin = "1111", role = role)
        auth.signIn("u1", "1111")
    }

    private fun viewModel(itemId: String? = null) = MenuItemEditorViewModel(
        savedStateHandle = SavedStateHandle(itemId?.let { mapOf(MENU_ITEM_ID_ARG to it) } ?: emptyMap()),
        menuRepository = menu,
        inventory = inventory,
        authorise = authorise,
        storeProfiles = profiles,
    )

    @Test
    fun `a new item saves the item plus a variant priced in minor units`() = runTest {
        signInAs(StaffRole.MANAGER)
        menu.upsertCategory(Category(id = "c1", name = "Rice"))
        val vm = viewModel()

        vm.uiState.test {
            var state = awaitItem()
            while (state.loading) state = awaitItem()

            vm.edit { it.copy(name = "Nasi Goreng") }
            vm.editVariant(state.draft.variants.single().id) { it.copy(name = "Regular", priceText = "3.50") }
            vm.save()

            var latest = expectMostRecentItem()
            while (!latest.done) latest = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val savedItem = menu.observeItems("c1").first().single()
        assertEquals("Nasi Goreng", savedItem.name)
        val variants = menu.observeItemWithVariants(savedItem.id).first()!!.variants
        assertEquals(listOf(Money(350)), variants.map { it.price })
    }

    @Test
    fun `the recipe sheet saves the variant's ingredient lines`() = runTest {
        signInAs(StaffRole.MANAGER)
        menu.upsertCategory(Category(id = "c1", name = "Rice"))
        menu.saveItemWithVariants(
            MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng"),
            listOf(MenuVariant(id = "v1", menuItemId = "i1", name = "Regular", price = Money(1_500))),
            actorStaffId = "seed",
        )
        inventory.upsertIngredient(
            Ingredient(
                id = "rice",
                name = "Rice",
                baseUnit = BaseUnit.G,
                purchaseUnit = "sack",
                purchaseToBaseFactor = 25_000.0,
                currentStockBase = 10_000.0,
                avgCostPerBase = Money(10),
                lowStockThresholdBase = 0.0,
            ),
        )
        val vm = viewModel(itemId = "i1")

        vm.uiState.test {
            var state = awaitItem()
            while (state.loading) state = awaitItem()

            vm.openRecipe("v1")
            var withRecipe = awaitItem()
            while (withRecipe.recipe == null) withRecipe = awaitItem()

            vm.editRecipeRow(0) { it.copy(ingredientId = "rice", qtyText = "200") }
            vm.saveRecipe()
            var saved = awaitItem()
            while (saved.recipe != null) saved = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val recipe = inventory.getRecipe("v1")
        assertEquals(listOf("Rice" to 200.0), recipe.map { it.ingredient.name to it.line.qtyBase })
        assertEquals(Money(2_000), inventory.costOfVariant("v1")) // 200g × 10
    }

    @Test
    fun `editing an existing item loads its variants formatted in major units`() = runTest {
        signInAs(StaffRole.MANAGER)
        menu.upsertCategory(Category(id = "c1", name = "Rice"))
        menu.saveItemWithVariants(
            MenuItem(id = "i1", categoryId = "c1", name = "Indomie"),
            listOf(MenuVariant(id = "v1", menuItemId = "i1", name = "Regular", price = Money(1_250))),
            actorStaffId = "seed",
        )
        val vm = viewModel(itemId = "i1")

        vm.uiState.test {
            var state = awaitItem()
            while (state.loading) state = awaitItem()
            assertEquals("Indomie", state.draft.name)
            assertEquals("12.5", state.draft.variants.single().priceText)
            assertTrue(state.canSave)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a waiter cannot save or delete an item`() = runTest {
        signInAs(StaffRole.WAITER)
        menu.upsertCategory(Category(id = "c1", name = "Rice"))
        menu.upsertItem(MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng"))
        menu.upsertVariant(MenuVariant(id = "v1", menuItemId = "i1", name = "Regular", price = Money(2_500)))
        val vm = viewModel("i1")

        vm.uiState.test {
            var state = expectMostRecentItem()
            while (state.loading) state = awaitItem()

            assertFalse(state.canManage)
            // canSave folds the permission in, so the Save button is disabled too.
            assertFalse(state.canSave)

            vm.edit { it.copy(name = "Renamed") }
            vm.save()
            assertEquals("Nasi Goreng", menu.observeItemWithVariants("i1").first()?.item?.name)

            vm.delete()
            assertTrue(menu.observeItemWithVariants("i1").first() != null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `re-pricing a variant records the signed-in manager as the actor`() = runTest {
        signInAs(StaffRole.MANAGER)
        menu.upsertCategory(Category(id = "c1", name = "Rice"))
        menu.upsertItem(MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng"))
        menu.upsertVariant(MenuVariant(id = "v1", menuItemId = "i1", name = "Regular", price = Money(2_500)))
        val vm = viewModel("i1")

        vm.uiState.test {
            var state = expectMostRecentItem()
            while (state.loading) state = awaitItem()

            vm.editVariant("v1") { it.copy(priceText = "30.00") }
            vm.save()
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        val edit = menu.recordedPriceEdits.single()
        assertEquals("v1", edit.variantId)
        assertEquals(Money(2_500), edit.before)
        assertEquals(Money(3_000), edit.after)
        assertEquals("u1", edit.actorStaffId)
    }
}
