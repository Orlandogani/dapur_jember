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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.domain.backup.BackupFile
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

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Backup & restore") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PosOutlinedButton(text = "Back", onClick = onBack, modifier = Modifier.weight(1f))
                PosButton(
                    text = "Back up now",
                    onClick = viewModel::startCreate,
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                "Backups are encrypted with a passphrase you choose. Without that passphrase " +
                    "a backup cannot be restored — write it down somewhere safe.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            if (state.backups.isEmpty()) {
                Text("No backups yet.", style = MaterialTheme.typography.bodyMedium)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.backups, key = { it.path }) { file ->
                    BackupRow(file = file, onRestore = { viewModel.startRestore(file) }, enabled = !state.busy)
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
private fun BackupRow(file: BackupFile, onRestore: () -> Unit, enabled: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(STAMP.format(Instant.ofEpochMilli(file.createdAt)), style = MaterialTheme.typography.titleMedium)
            Text("${file.sizeBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
            PosOutlinedButton(
                text = "Restore…",
                onClick = onRestore,
                enabled = enabled,
                modifier = Modifier.padding(top = 8.dp),
            )
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
        confirmButton = { PosButton(text = "Create backup", onClick = onConfirm, enabled = draft.canCreate) },
        dismissButton = { PosOutlinedButton(text = "Cancel", onClick = onDismiss) },
        title = { Text("New backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = draft.passphrase,
                    onValueChange = { v -> onChange { it.copy(passphrase = v) } },
                    label = { Text("Passphrase (at least 8 characters)") },
                    isError = draft.tooShort,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = draft.confirm,
                    onValueChange = { v -> onChange { it.copy(confirm = v) } },
                    label = { Text("Repeat passphrase") },
                    isError = draft.mismatch,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                if (draft.mismatch) {
                    Text(
                        "The two passphrases do not match.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    "There is no way to recover a backup if you forget this passphrase.",
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
        confirmButton = { PosButton(text = "Overwrite and restore", onClick = onConfirm, enabled = draft.canRestore) },
        dismissButton = { PosOutlinedButton(text = "Cancel", onClick = onDismiss) },
        title = { Text("Restore backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "This replaces everything currently on this tablet — all orders, menu and " +
                        "stock — with the contents of ${draft.file.name}. It cannot be undone.",
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedTextField(
                    value = draft.passphrase,
                    onValueChange = { v -> onChange { it.copy(passphrase = v) } },
                    label = { Text("Backup passphrase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = draft.acknowledged,
                        onCheckedChange = { v -> onChange { it.copy(acknowledged = v) } },
                    )
                    Text("I understand the current data will be lost.")
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
            PosButton(text = "Close app", onClick = { android.os.Process.killProcess(android.os.Process.myPid()) })
        },
        title = { Text("Restore complete") },
        text = { Text("Close and reopen DapurJember to finish restoring.") },
    )
}
