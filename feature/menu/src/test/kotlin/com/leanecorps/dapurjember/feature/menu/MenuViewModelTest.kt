package com.leanecorps.dapurjember.feature.menu

import app.cash.turbine.test
import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.ObserveMenuUseCase
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeMenuRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MenuViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repository = FakeMenuRepository()

    private fun viewModel() = MenuViewModel(ObserveMenuUseCase(repository), repository)

    @Test
    fun `uiState groups items by category once the menu loads`() = runTest {
        repository.upsertCategory(Category(id = "c1", name = "Drinks", sortOrder = 1))
        repository.upsertItem(MenuItem(id = "i1", categoryId = "c1", name = "Es Teh"))

        viewModel().uiState.test {
            val loaded = expectMostRecentItem()
            assertFalse(loaded.loading)
            assertEquals(listOf("Drinks"), loaded.sections.map { it.name })
            assertEquals(listOf("Es Teh"), loaded.sections.single().items.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setAvailability flips the sold-out flag`() = runTest {
        repository.upsertCategory(Category(id = "c1", name = "Food"))
        repository.upsertItem(MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng", available = true))
        val vm = viewModel()

        vm.uiState.test {
            assertEquals(true, expectMostRecentItem().sections.single().items.single().available)
            vm.setAvailability("i1", available = false)
            assertEquals(false, awaitItem().sections.single().items.single().available)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
