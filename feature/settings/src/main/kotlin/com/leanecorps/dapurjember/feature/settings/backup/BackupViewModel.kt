package com.leanecorps.dapurjember.feature.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.domain.backup.BackupFile
import com.leanecorps.dapurjember.core.domain.backup.BackupRepository
import com.leanecorps.dapurjember.core.domain.backup.RestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MIN_PASSPHRASE = 8

data class BackupUiState(
    val backups: List<BackupFile> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val createDialog: CreateBackupDraft? = null,
    val restoreDialog: RestoreDraft? = null,
    val restartRequired: Boolean = false,
)

data class CreateBackupDraft(val passphrase: String = "", val confirm: String = "") {
    val tooShort: Boolean get() = passphrase.isNotEmpty() && passphrase.length < MIN_PASSPHRASE
    val mismatch: Boolean get() = confirm.isNotEmpty() && confirm != passphrase
    val canCreate: Boolean get() = passphrase.length >= MIN_PASSPHRASE && confirm == passphrase
}

/** Restore is destructive, so the user must both enter the passphrase and tick the confirmation. */
data class RestoreDraft(
    val file: BackupFile,
    val passphrase: String = "",
    val acknowledged: Boolean = false,
) {
    val canRestore: Boolean get() = passphrase.isNotEmpty() && acknowledged
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backups: BackupRepository,
) : ViewModel() {

    private val local = MutableStateFlow(BackupUiState())

    val uiState: StateFlow<BackupUiState> = combine(backups.observeBackups(), local) { files, state ->
        state.copy(backups = files)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), BackupUiState())

    fun startCreate() = local.update { it.copy(createDialog = CreateBackupDraft(), message = null) }

    fun editCreate(transform: (CreateBackupDraft) -> CreateBackupDraft) =
        local.update { state -> state.copy(createDialog = state.createDialog?.let(transform)) }

    fun cancelCreate() = local.update { it.copy(createDialog = null) }

    fun confirmCreate() {
        val draft = local.value.createDialog ?: return
        if (!draft.canCreate) return
        local.update { it.copy(busy = true, createDialog = null) }
        viewModelScope.launch {
            val result = runCatching { backups.createBackup(draft.passphrase.toCharArray()) }
            backups.pruneOldBackups()
            local.update {
                it.copy(
                    busy = false,
                    message = result.fold(
                        onSuccess = { file -> "Backup saved: ${file.name}" },
                        onFailure = { e -> "Backup failed: ${e.message}" },
                    ),
                )
            }
        }
    }

    fun startRestore(file: BackupFile) =
        local.update { it.copy(restoreDialog = RestoreDraft(file), message = null) }

    fun editRestore(transform: (RestoreDraft) -> RestoreDraft) =
        local.update { state -> state.copy(restoreDialog = state.restoreDialog?.let(transform)) }

    fun cancelRestore() = local.update { it.copy(restoreDialog = null) }

    fun confirmRestore() {
        val draft = local.value.restoreDialog ?: return
        if (!draft.canRestore) return
        local.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val result = backups.restore(draft.file, draft.passphrase.toCharArray())) {
                is RestoreResult.RestartRequired -> local.update {
                    it.copy(busy = false, restoreDialog = null, restartRequired = true)
                }

                is RestoreResult.Failed -> local.update {
                    it.copy(busy = false, restoreDialog = draft.copy(passphrase = ""), message = result.message)
                }
            }
        }
    }

    fun dismissMessage() = local.update { it.copy(message = null) }

    private inline fun MutableStateFlow<BackupUiState>.update(transform: (BackupUiState) -> BackupUiState) {
        value = transform(value)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
