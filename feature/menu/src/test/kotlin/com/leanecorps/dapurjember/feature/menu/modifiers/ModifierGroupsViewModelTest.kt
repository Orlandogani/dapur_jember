package com.leanecorps.dapurjember.feature.menu.modifiers

import com.leanecorps.dapurjember.core.domain.config.StoreProfile
import com.leanecorps.dapurjember.core.domain.pricing.RoundingRule
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeMenuRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeStoreProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ModifierGroupsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val menu = FakeMenuRepository()
    private val profiles = FakeStoreProfileRepository(
        StoreProfile(
            id = "p1",
            name = "T",
            currencyCode = "IDR",
            currencyMinorUnits = 0,
            taxRateBasisPoints = 0,
            taxInclusive = false,
            serviceChargeBasisPoints = 0,
            serviceChargeTaxable = false,
            roundingRule = RoundingRule.NONE,
            businessDayCutoffMinutes = 0,
            timezoneId = "UTC",
        ),
    )
    private val viewModel by lazy { ModifierGroupsViewModel(menu, profiles) }

    @Test
    fun `adding a group with options persists it`() = runTest {
        viewModel.startAdd()
        viewModel.edit { it.copy(name = "Spice level", required = true, minSelectText = "1", maxSelectText = "1") }
        val firstRowId = viewModel.currentEditor!!.modifiers.single().id
        viewModel.editModifier(firstRowId) { it.copy(name = "Mild") }
        viewModel.addModifier()
        val secondRowId = viewModel.currentEditor!!.modifiers.last().id
        viewModel.editModifier(secondRowId) { it.copy(name = "Hot", priceDeltaText = "2000") }
        viewModel.save()
        advanceUntilIdle()

        val group = menu.observeModifierGroups().first().single()
        assertEquals("Spice level", group.name)
        assertTrue(group.required)
        val detail = menu.observeModifierGroup(group.id).first()!!
        assertEquals(listOf("Mild" to 0L, "Hot" to 2_000L), detail.modifiers.map { it.name to it.priceDelta.minor })
    }

    @Test
    fun `save is a no-op without a name`() = runTest {
        viewModel.startAdd()
        viewModel.editModifier(viewModel.currentEditor!!.modifiers.single().id) { it.copy(name = "X") }
        viewModel.save()
        advanceUntilIdle()

        assertTrue(menu.observeModifierGroups().first().isEmpty())
    }
}
