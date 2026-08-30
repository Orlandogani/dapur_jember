package com.leanecorps.dapurjember.feature.menu.csv

import com.leanecorps.dapurjember.core.domain.auth.AuthoriseUseCase
import com.leanecorps.dapurjember.core.domain.auth.StaffRole
import com.leanecorps.dapurjember.core.domain.config.StoreProfile
import com.leanecorps.dapurjember.core.domain.menu.ImportMenuCsvUseCase
import com.leanecorps.dapurjember.core.domain.pricing.RoundingRule
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeAuthRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeMenuRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeSessionRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeStoreProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ImportMenuViewModelTest {

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
    private val session = FakeSessionRepository()
    private val auth = FakeAuthRepository(session)
    private val authorise = AuthoriseUseCase(auth, session)

    /** Signs someone in, so the MANAGE_MENU check has a role to look at. */
    private suspend fun signInAs(role: StaffRole) {
        auth.addStaff(id = "u1", name = "Staff", pin = "1111", role = role)
        auth.signIn("u1", "1111")
    }

    private fun viewModel() = ImportMenuViewModel(ImportMenuCsvUseCase(menu, profiles), authorise)

    @Test
    fun `a manager can import a menu`() = runTest {
        signInAs(StaffRole.MANAGER)
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.canManage)

        vm.setText("category,item,variant,price\nRice,Nasi Goreng,Regular,25000")
        vm.import()
        advanceUntilIdle()

        assertTrue(menu.observeCategories().first().any { it.name == "Rice" })
    }

    @Test
    fun `a waiter cannot import a menu`() = runTest {
        signInAs(StaffRole.WAITER)
        val vm = viewModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.canManage)

        vm.setText("category,item,variant,price\nRice,Nasi Goreng,Regular,25000")
        vm.import()
        advanceUntilIdle()

        assertNull(vm.uiState.value.summary)
        assertTrue(menu.observeCategories().first().isEmpty())
    }
}
