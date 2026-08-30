package com.leanecorps.dapurjember.feature.settings.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.domain.auth.AuthRepository
import com.leanecorps.dapurjember.core.domain.auth.AuthoriseUseCase
import com.leanecorps.dapurjember.core.domain.auth.Permission
import com.leanecorps.dapurjember.core.domain.auth.Staff
import com.leanecorps.dapurjember.core.domain.auth.StaffRole
import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PIN_MIN = 4
private const val PIN_MAX = 6

data class StaffUiState(
    val loading: Boolean = true,
    val canManage: Boolean = false,
    val currentStaffId: String? = null,
    val staff: List<Staff> = emptyList(),
    val editor: StaffDraft? = null,
    val message: String? = null,
)

data class StaffDraft(
    val id: String? = null,
    val name: String = "",
    val role: StaffRole = StaffRole.WAITER,
    val pin: String = "",
    val active: Boolean = true,
) {
    val isNew: Boolean get() = id == null

    /** A new member needs a PIN; editing an existing one may leave it blank to keep the old PIN. */
    val pinValid: Boolean
        get() = when {
            isNew -> pin.length in PIN_MIN..PIN_MAX
            pin.isEmpty() -> true
            else -> pin.length in PIN_MIN..PIN_MAX
        }

    val canSave: Boolean get() = name.isNotBlank() && pinValid
}

@HiltViewModel
class StaffViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val authorise: AuthoriseUseCase,
    private val session: SessionRepository,
) : ViewModel() {

    private val editor = MutableStateFlow<StaffDraft?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val canManage = MutableStateFlow(false)
    private val currentStaffId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StaffUiState> = combine(
        auth.observeAllStaff(),
        editor,
        message,
        canManage,
        currentStaffId,
    ) { staff, editorState, msg, allowed, currentId ->
        StaffUiState(
            loading = false,
            canManage = allowed,
            currentStaffId = currentId,
            staff = staff,
            editor = editorState,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StaffUiState())

    init {
        viewModelScope.launch {
            canManage.value = authorise.currentUserCan(Permission.MANAGE_STAFF)
            currentStaffId.value = session.currentStaffId()
        }
    }

    fun startAdd() {
        editor.value = StaffDraft()
    }

    fun startEdit(staff: Staff) {
        editor.value = StaffDraft(id = staff.id, name = staff.name, role = staff.role, active = staff.active)
    }

    fun edit(transform: (StaffDraft) -> StaffDraft) {
        editor.value = editor.value?.let(transform)
    }

    fun closeEditor() {
        editor.value = null
    }

    fun save() {
        val draft = editor.value?.takeIf { it.canSave && canManage.value } ?: return
        val actorId = currentStaffId.value ?: return
        viewModelScope.launch {
            if (draft.isNew) {
                auth.createStaff(draft.name.trim(), draft.role, draft.pin)
            } else {
                auth.updateStaff(draft.id!!, draft.name.trim(), draft.role, actorId)
                if (draft.pin.isNotEmpty()) auth.resetPin(draft.id, draft.pin, actorId)
            }
            editor.value = null
        }
    }

    fun setActive(staff: Staff, active: Boolean) {
        val actorId = currentStaffId.value?.takeIf { canManage.value } ?: return
        // Locking yourself out of the only account that can manage staff is unrecoverable
        // without a backup, so refuse it rather than letting the owner do it by accident.
        if (!active && staff.id == actorId) {
            message.value = "You cannot deactivate the account you are signed in with."
        } else {
            viewModelScope.launch { auth.setStaffActive(staff.id, active, actorId) }
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
