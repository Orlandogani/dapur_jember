package com.leanecorps.dapurjember.feature.settings.setup

import app.cash.turbine.test
import com.leanecorps.dapurjember.core.domain.auth.StaffRole
import com.leanecorps.dapurjember.core.domain.config.CompleteSetupUseCase
import com.leanecorps.dapurjember.core.domain.pricing.RoundingRule
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeAuthRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeStoreProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SetupWizardViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val profiles = FakeStoreProfileRepository()
    private val auth = FakeAuthRepository()
    private val viewModel = SetupWizardViewModel(CompleteSetupUseCase(profiles, auth))

    @Test
    fun `the tax step rejects a non-numeric or out-of-range rate`() = runTest {
        viewModel.setTaxPercent("abc")
        assertFalse(viewModel.uiState.value.canContinueTax)

        viewModel.setTaxPercent("150")
        assertFalse(viewModel.uiState.value.canContinueTax)

        viewModel.setTaxPercent("11")
        assertTrue(viewModel.uiState.value.canContinueTax)
    }

    @Test
    fun `finish writes the profile and creates the owner`() = runTest {
        viewModel.setRestaurantName("Dapur Jember")
        viewModel.setCurrency(CurrencyChoice.IDR)
        viewModel.setTaxPercent("11")
        viewModel.setServiceChargePercent("5")
        viewModel.setOwnerName("Pak Budi")
        viewModel.setOwnerPin("1357")

        viewModel.uiState.test {
            viewModel.finish()
            var state = awaitItem()
            while (!state.done) state = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val profile = profiles.getProfile()!!
        assertEquals("Dapur Jember", profile.name)
        assertEquals("IDR", profile.currencyCode)
        assertEquals(0, profile.currencyMinorUnits)
        assertEquals(1_100, profile.taxRateBasisPoints)
        assertEquals(500, profile.serviceChargeBasisPoints)
        assertEquals(RoundingRule.NEAREST_100, profile.roundingRule)

        val owner = auth.observeActiveStaff().first().single()
        assertEquals("Pak Budi", owner.name)
        assertEquals(StaffRole.OWNER, owner.role)
        assertTrue(auth.verifyPin(owner.id, "1357"))
    }

    @Test
    fun `finish does nothing without a valid owner PIN`() = runTest {
        viewModel.setRestaurantName("X")
        viewModel.setOwnerName("Y")
        viewModel.setOwnerPin("12")

        viewModel.finish()

        assertEquals(null, profiles.getProfile())
    }
}
