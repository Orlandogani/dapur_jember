@file:OptIn(ExperimentalMaterial3Api::class)

package com.leanecorps.dapurjember.feature.settings.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.domain.backup.BackupFile
import com.leanecorps.dapurjember.core.domain.backup.RestoreFailure
import com.leanecorps.dapurjember.feature.settings.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val STAMP: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    val messageText = state.message?.text()
    LaunchedEffect(messageText) {
        messageText?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.backup_title)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PosOutlinedButton(
                    text = stringResource(R.string.backup_action_back),
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                PosButton(
                    text = stringResource(R.string.backup_action_now),
                    onClick = viewModel::startCreate,
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                stringResource(R.string.backup_help),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            if (state.backups.isEmpty()) {
                Text(stringResource(R.string.backup_empty), style = MaterialTheme.typography.bodyMedium)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.backups, key = { it.path }) { file ->
                    BackupRow(
                        file = file,
                        onRestore = { viewModel.startRestore(file) },
                        onShare = { shareBackup(context, file) },
                        enabled = !state.busy,
                    )
                }
            }
        }
    }

    state.createDialog?.let { draft ->
        CreateBackupDialog(
            draft = draft,
            onChange = viewModel::editCreate,
            onDismiss = viewModel::cancelCreate,
            onConfirm = viewModel::confirmCreate,
        )
    }
    state.restoreDialog?.let { draft ->
        RestoreDialog(
            draft = draft,
            onChange = viewModel::editRestore,
            onDismiss = viewModel::cancelRestore,
            onConfirm = viewModel::confirmRestore,
        )
    }
    if (state.restartRequired) RestartRequiredDialog()
}

@Composable
private fun BackupRow(file: BackupFile, onRestore: () -> Unit, onShare: () -> Unit, enabled: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(STAMP.format(Instant.ofEpochMilli(file.createdAt)), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    if (file.isAutomatic) R.string.backup_size_automatic else R.string.backup_size,
                    file.sizeBytes / BYTES_PER_KB,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosOutlinedButton(
                    text = stringResource(R.string.backup_action_restore),
                    onClick = onRestore,
                    enabled = enabled,
                )
                // An automatic backup is encrypted with a device key, so it is useless
                // elsewhere — only a passphrase-protected manual backup is worth sharing.
                if (!file.isAutomatic) {
                    PosOutlinedButton(
                        text = stringResource(R.string.backup_action_share),
                        onClick = onShare,
                        enabled = enabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateBackupDialog(
    draft: CreateBackupDraft,
    onChange: ((CreateBackupDraft) -> CreateBackupDraft) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PosButton(
                text = stringResource(R.string.backup_action_create),
                onClick = onConfirm,
                enabled = draft.canCreate,
            )
        },
        dismissButton = {
            PosOutlinedButton(
                text = stringResource(R.string.backup_action_cancel),
                onClick = onDismiss,
            )
        },
        title = { Text(stringResource(R.string.backup_create_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = draft.passphrase,
                    onValueChange = { v -> onChange { it.copy(passphrase = v) } },
                    label = { Text(stringResource(R.string.backup_passphrase_label, MIN_PASSPHRASE)) },
                    isError = draft.tooShort,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = draft.confirm,
                    onValueChange = { v -> onChange { it.copy(confirm = v) } },
                    label = { Text(stringResource(R.string.backup_passphrase_repeat_label)) },
                    isError = draft.mismatch,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                if (draft.mismatch) {
                    Text(
                        stringResource(R.string.backup_passphrase_mismatch),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    stringResource(R.string.backup_passphrase_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
private fun RestoreDialog(
    draft: RestoreDraft,
    onChange: ((RestoreDraft) -> RestoreDraft) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PosButton(
                text = stringResource(R.string.backup_action_overwrite),
                onClick = onConfirm,
                enabled = draft.canRestore,
            )
        },
        dismissButton = {
            PosOutlinedButton(
                text = stringResource(R.string.backup_action_cancel),
                onClick = onDismiss,
            )
        },
        title = { Text(stringResource(R.string.backup_restore_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.backup_restore_warning, draft.file.name),
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedTextField(
                    value = draft.passphrase,
                    onValueChange = { v -> onChange { it.copy(passphrase = v) } },
                    label = { Text(stringResource(R.string.backup_restore_passphrase_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = draft.acknowledged,
                        onCheckedChange = { v -> onChange { it.copy(acknowledged = v) } },
                    )
                    Text(stringResource(R.string.backup_restore_acknowledge))
                }
            }
        },
    )
}

/** The DB file was swapped underneath Room; only a fresh process can safely reopen it. */
@Composable
private fun RestartRequiredDialog() {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            PosButton(
                text = stringResource(R.string.backup_action_close_app),
                onClick = { android.os.Process.killProcess(android.os.Process.myPid()) },
            )
        },
        title = { Text(stringResource(R.string.backup_restart_title)) },
        text = { Text(stringResource(R.string.backup_restart_body)) },
    )
}

/** Renders a [BackupMessage] for the snackbar; the wording lives in `strings.xml` (NFR8). */
@Composable
private fun BackupMessage.text(): String = when (this) {
    is BackupMessage.Saved -> stringResource(R.string.backup_saved, fileName)
    BackupMessage.CreateFailed -> stringResource(R.string.backup_create_failed)
    is BackupMessage.RestoreFailed -> when (reason) {
        RestoreFailure.UNREADABLE -> stringResource(R.string.backup_restore_unreadable)
        RestoreFailure.PREVIOUS_DATA_KEPT -> stringResource(R.string.backup_restore_kept)
    }
}

private const val BYTES_PER_KB = 1024
