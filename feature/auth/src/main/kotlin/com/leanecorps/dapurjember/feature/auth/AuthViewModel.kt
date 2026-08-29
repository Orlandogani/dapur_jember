package com.leanecorps.dapurjember.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.domain.auth.AuthRepository
import com.leanecorps.dapurjember.core.domain.auth.StaffRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val state = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeActiveStaff().collect { staff ->
                state.update { it.copy(loading = false, staff = staff) }
            }
        }
    }

    fun selectStaff(staffId: String) = state.update { it.copy(selectedStaffId = staffId, pin = "", error = false) }

    fun appendDigit(digit: Char) = state.update {
        if (it.pin.length >= AuthUiState.PIN_MAX) it else it.copy(pin = it.pin + digit, error = false)
    }

    fun deleteDigit() = state.update { it.copy(pin = it.pin.dropLast(1), error = false) }

    fun submit() {
        val current = state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            val ok = if (current.firstRun) {
                val id = authRepository.createStaff(name = "Owner", role = StaffRole.OWNER, pin = current.pin)
                authRepository.signIn(id, current.pin)
            } else {
                authRepository.signIn(current.selectedStaffId!!, current.pin)
            }
            state.update { it.copy(signedIn = ok, error = !ok, pin = if (ok) it.pin else "") }
        }
    }
}
