package com.leanecorps.dapurjember.feature.auth

import app.cash.turbine.test
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeAuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AuthViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val auth = FakeAuthRepository()
    private fun viewModel() = AuthViewModel(auth)

    private fun AuthViewModel.type(pin: String) = pin.forEach { appendDigit(it) }

    @Test
    fun `first run creates the owner and signs in`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertTrue(expectMostRecentItem().firstRun)
            vm.type("1234")
            vm.submit()
            assertTrue(expectMostRecentItem().signedIn)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("Owner"), auth.observeActiveStaff().first().map { it.name })
    }

    @Test
    fun `a wrong PIN sets the error flag and clears the entry`() = runTest {
        auth.addStaff(id = "s1", name = "Sari", pin = "4321")
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            vm.selectStaff("s1")
            vm.type("0000")
            vm.submit()
            val after = expectMostRecentItem()
            assertFalse(after.signedIn)
            assertTrue(after.error)
            assertEquals("", after.pin)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the right PIN signs the selected staff in`() = runTest {
        auth.addStaff(id = "s1", name = "Sari", pin = "4321")
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            vm.selectStaff("s1")
            vm.type("4321")
            vm.submit()
            assertTrue(expectMostRecentItem().signedIn)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("s1", auth.sessionRepository().currentSession()?.staffId)
    }
}
