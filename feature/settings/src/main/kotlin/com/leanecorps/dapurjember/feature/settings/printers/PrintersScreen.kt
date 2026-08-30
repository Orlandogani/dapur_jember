@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.leanecorps.dapurjember.feature.settings.printers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.domain.printing.PrinterLink
import com.leanecorps.dapurjember.core.domain.printing.PrinterRole

@Composable
fun PrintersScreen(
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenStaff: () -> Unit,
    viewModel: PrintersViewModel = hiltViewModel(),
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
        topBar = { TopAppBar(title = { Text("Printers") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PosOutlinedButton(text = "Back", onClick = onBack, modifier = Modifier.weight(1f))
                PosOutlinedButton(text = "Backup", onClick = onOpenBackup, modifier = Modifier.weight(1f))
                PosOutlinedButton(text = "Staff", onClick = onOpenStaff, modifier = Modifier.weight(1f))
                PosButton(text = "Add printer", onClick = viewModel::startAdd, modifier = Modifier.weight(1f))
            }

            if (state.printers.isEmpty() && !state.loading) {
                Text(
                    "No printers yet. Add one so tickets and receipts have somewhere to go.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.printers, key = { it.id }) { row ->
                    PrinterCard(
                        row = row,
                        onEdit = { viewModel.startEdit(row.id) },
                        onTest = { viewModel.testPrint(row.id) },
                        onDelete = { viewModel.delete(row.id) },
                    )
                }
            }
        }
    }

    state.editor?.let { editor ->
        PrinterEditorDialog(
            editor = editor,
            onChange = viewModel::edit,
            onToggleRole = viewModel::toggleRole,
            onSetLink = viewModel::setLink,
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::save,
        )
    }
}

@Composable
private fun PrinterCard(row: PrinterRowUi, onEdit: () -> Unit, onTest: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(row.name, style = MaterialTheme.typography.titleMedium)
            Text("${row.transport} · ${row.address}", style = MaterialTheme.typography.bodySmall)
            Text(row.roles, style = MaterialTheme.typography.labelMedium)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosOutlinedButton(text = "Edit", onClick = onEdit)
                PosOutlinedButton(text = "Test print", onClick = onTest)
                PosOutlinedButton(text = "Remove", onClick = onDelete)
            }
        }
    }
}

@Composable
private fun PrinterEditorDialog(
    editor: PrinterEditorUi,
    onChange: ((PrinterEditorUi) -> PrinterEditorUi) -> Unit,
    onToggleRole: (PrinterRole) -> Unit,
    onSetLink: (PrinterLink) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { PosButton(text = "Save", onClick = onSave, enabled = editor.canSave) },
        dismissButton = { PosOutlinedButton(text = "Cancel", onClick = onDismiss) },
        title = { Text(if (editor.isNew) "Add printer" else "Edit printer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { v -> onChange { it.copy(name = v) } },
                    label = { Text("Name") },
                    singleLine = true,
                )
                ChipRow("Connection", PrinterLink.entries, editor.link) { onSetLink(it) }
                OutlinedTextField(
                    value = editor.address,
                    onValueChange = { v -> onChange { it.copy(address = v) } },
                    label = { Text(addressLabel(editor.link)) },
                    singleLine = true,
                )
                ChipRow("Paper width", listOf(58, 80), editor.paperWidthMm) { mm ->
                    onChange { it.copy(paperWidthMm = mm) }
                }
                OutlinedTextField(
                    value = editor.codepageText,
                    onValueChange = { v -> onChange { it.copy(codepageText = v) } },
                    label = { Text("Codepage (ESC t)") },
                    singleLine = true,
                    isError = editor.codepage == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text("Roles", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrinterRole.entries.forEach { role ->
                        FilterChip(
                            selected = role in editor.roles,
                            onClick = { onToggleRole(role) },
                            label = { Text(role.name) },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun <T> ChipRow(label: String, options: List<T>, selected: T, onSelect: (T) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(option.toString()) },
                )
            }
        }
    }
}

private fun addressLabel(link: PrinterLink): String = when (link) {
    PrinterLink.TCP -> "Host or host:port"
    PrinterLink.BLUETOOTH -> "Bluetooth MAC address"
    PrinterLink.USB -> "USB device name"
}
