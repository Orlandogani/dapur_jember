package com.leanecorps.dapurjember.feature.menu

import app.cash.turbine.test
import com.leanecorps.dapurjember.core.domain.auth.AuthoriseUseCase
import com.leanecorps.dapurjember.core.domain.auth.StaffRole
import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.ObserveMenuUseCase
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeAuthRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeMenuRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeSessionRepository
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
class MenuViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repository = FakeMenuRepository()

    private val session = FakeSessionRepository()
    private val auth = FakeAuthRepository(session)
    private val authorise = AuthoriseUseCase(auth, session)

    /** Signs someone in, so the MANAGE_MENU check has a role to look at. */
    private suspend fun signInAs(role: StaffRole) {
        auth.addStaff(id = "u1", name = "Staff", pin = "1111", role = role)
        auth.signIn("u1", "1111")
    }

    private fun viewModel() = MenuViewModel(ObserveMenuUseCase(repository), repository, authorise)

    @Test
    fun `uiState groups items by category once the menu loads`() = runTest {
        signInAs(StaffRole.MANAGER)
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
        signInAs(StaffRole.WAITER)
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

    @Test
    fun `a waiter may flip sold-out but may not add a category`() = runTest {
        signInAs(StaffRole.WAITER)
        repository.upsertCategory(Category(id = "c1", name = "Food"))
        val vm = viewModel()

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.canManage)

            vm.addCategory("Desserts")
            expectNoEvents()
            assertEquals(listOf("Food"), repository.observeCategories().first().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a manager may add a category`() = runTest {
        signInAs(StaffRole.MANAGER)
        repository.upsertCategory(Category(id = "c1", name = "Food"))
        val vm = viewModel()

        vm.uiState.test {
            assertTrue(expectMostRecentItem().canManage)

            vm.addCategory("Desserts")
            advanceUntilIdle()
            assertEquals(
                listOf("Food", "Desserts"),
                repository.observeCategories().first().map { it.name },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}
