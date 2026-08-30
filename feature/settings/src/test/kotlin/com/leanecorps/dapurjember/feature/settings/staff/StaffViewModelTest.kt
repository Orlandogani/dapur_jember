package com.leanecorps.dapurjember.feature.settings.staff

import com.leanecorps.dapurjember.core.domain.auth.AuthoriseUseCase
import com.leanecorps.dapurjember.core.domain.auth.StaffRole
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class StaffViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val auth = FakeAuthRepository()
    private val session = auth.sessionRepository()
    private val viewModel by lazy { StaffViewModel(auth, AuthoriseUseCase(auth, session), session) }

    private fun TestScope.subscribe() {
        backgroundScope.launch { viewModel.uiState.collect { } }
    }

    private suspend fun signInAs(role: StaffRole, id: String = "me") {
        auth.addStaff(id = id, name = "Boss", pin = "1111", role = role)
        auth.signIn(id, "1111")
    }

    @Test
    fun `an owner can add a staff member`() = runTest {
        signInAs(StaffRole.OWNER)
        subscribe()
        advanceUntilIdle()

        viewModel.startAdd()
        viewModel.edit { it.copy(name = "Wira", role = StaffRole.WAITER, pin = "2222") }
        viewModel.save()
        advanceUntilIdle()

        assertTrue(auth.observeAllStaff().first().any { it.name == "Wira" && it.role == StaffRole.WAITER })
    }

    @Test
    fun `a manager cannot manage staff and the screen says so`() = runTest {
        signInAs(StaffRole.MANAGER)
        subscribe()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canManage)

        // Even if the UI were bypassed, the ViewModel refuses.
        viewModel.startAdd()
        viewModel.edit { it.copy(name = "Sneaky", pin = "3333") }
        viewModel.save()
        advanceUntilIdle()

        assertFalse(auth.observeAllStaff().first().any { it.name == "Sneaky" })
    }

    @Test
    fun `deactivating another staff member works and is reversible`() = runTest {
        signInAs(StaffRole.OWNER)
        auth.addStaff(id = "w1", name = "Wira", pin = "2222", role = StaffRole.WAITER)
        subscribe()
        advanceUntilIdle()

        val wira = auth.observeAllStaff().first().first { it.id == "w1" }
        viewModel.setActive(wira, active = false)
        advanceUntilIdle()
        assertFalse(auth.observeAllStaff().first().first { it.id == "w1" }.active)

        viewModel.setActive(wira, active = true)
        advanceUntilIdle()
        assertTrue(auth.observeAllStaff().first().first { it.id == "w1" }.active)
    }

    @Test
    fun `you cannot deactivate the account you are signed in with`() = runTest {
        signInAs(StaffRole.OWNER, id = "me")
        subscribe()
        advanceUntilIdle()

        val self = auth.observeAllStaff().first().first { it.id == "me" }
        viewModel.setActive(self, active = false)
        advanceUntilIdle()

        assertTrue(auth.observeAllStaff().first().first { it.id == "me" }.active)
        assertEquals(StaffMessage.CANNOT_DEACTIVATE_SELF, viewModel.uiState.value.message)
    }

    @Test
    fun `editing keeps the old PIN when the field is left blank`() = runTest {
        signInAs(StaffRole.OWNER)
        auth.addStaff(id = "w1", name = "Wira", pin = "2222", role = StaffRole.WAITER)
        subscribe()
        advanceUntilIdle()

        val wira = auth.observeAllStaff().first().first { it.id == "w1" }
        viewModel.startEdit(wira)
        viewModel.edit { it.copy(name = "Wira Santoso") }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.editor!!.canSave)
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Wira Santoso", auth.observeAllStaff().first().first { it.id == "w1" }.name)
        assertTrue(auth.verifyPin("w1", "2222")) // unchanged
    }
}
