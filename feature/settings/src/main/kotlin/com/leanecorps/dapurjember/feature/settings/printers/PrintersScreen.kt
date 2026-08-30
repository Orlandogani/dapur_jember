@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.leanecorps.dapurjember.feature.settings.printers

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.domain.printing.PrinterLink
import com.leanecorps.dapurjember.core.domain.printing.PrinterRole
import com.leanecorps.dapurjember.feature.settings.R

@Composable
fun PrintersScreen(
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenStaff: () -> Unit,
    viewModel: PrintersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val testQueued = state.testPageQueuedFor
    val testQueuedText = testQueued?.let { stringResource(R.string.printers_test_queued, it) }
    LaunchedEffect(testQueuedText) {
        testQueuedText?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.printers_title)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PosOutlinedButton(
                    text = stringResource(R.string.printers_action_back),
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                PosOutlinedButton(
                    text = stringResource(R.string.printers_action_backup),
                    onClick = onOpenBackup,
                    modifier = Modifier.weight(1f),
                )
                PosOutlinedButton(
                    text = stringResource(R.string.printers_action_staff),
                    onClick = onOpenStaff,
                    modifier = Modifier.weight(1f),
                )
                PosButton(
                    text = stringResource(R.string.printers_action_add),
                    onClick = viewModel::startAdd,
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.printers.isEmpty() && !state.loading) {
                Text(
                    stringResource(R.string.printers_empty),
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
            Text(
                stringResource(R.string.printers_connection_summary, stringResource(row.link.labelRes()), row.address),
                style = MaterialTheme.typography.bodySmall,
            )
            val roleLabels = row.roles.map { stringResource(it.labelRes()) }
            Text(roleLabels.joinToString(SEPARATOR), style = MaterialTheme.typography.labelMedium)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosOutlinedButton(text = stringResource(R.string.printers_action_edit), onClick = onEdit)
                PosOutlinedButton(text = stringResource(R.string.printers_action_test), onClick = onTest)
                PosOutlinedButton(text = stringResource(R.string.printers_action_remove), onClick = onDelete)
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
        confirmButton = {
            PosButton(text = stringResource(R.string.printers_action_save), onClick = onSave, enabled = editor.canSave)
        },
        dismissButton = {
            PosOutlinedButton(
                text = stringResource(R.string.printers_action_cancel),
                onClick = onDismiss,
            )
        },
        title = {
            Text(
                stringResource(
                    if (editor.isNew) R.string.printers_editor_new else R.string.printers_editor_edit,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { v -> onChange { it.copy(name = v) } },
                    label = { Text(stringResource(R.string.printers_name_label)) },
                    singleLine = true,
                )
                ChipRow(
                    label = stringResource(R.string.printers_connection_heading),
                    options = PrinterLink.entries,
                    selected = editor.link,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelect = { onSetLink(it) },
                )
                OutlinedTextField(
                    value = editor.address,
                    onValueChange = { v -> onChange { it.copy(address = v) } },
                    label = { Text(stringResource(editor.link.addressLabelRes())) },
                    singleLine = true,
                )
                ChipRow(
                    label = stringResource(R.string.printers_paper_width_heading),
                    options = listOf(PAPER_58, PAPER_80),
                    selected = editor.paperWidthMm,
                    optionLabel = { it.toString() },
                    onSelect = { mm -> onChange { it.copy(paperWidthMm = mm) } },
                )
                OutlinedTextField(
                    value = editor.codepageText,
                    onValueChange = { v -> onChange { it.copy(codepageText = v) } },
                    label = { Text(stringResource(R.string.printers_codepage_label)) },
                    singleLine = true,
                    isError = editor.codepage == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(stringResource(R.string.printers_roles_heading), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrinterRole.entries.forEach { role ->
                        FilterChip(
                            selected = role in editor.roles,
                            onClick = { onToggleRole(role) },
                            label = { Text(stringResource(role.labelRes())) },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun <T> ChipRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(optionLabel(option)) },
                )
            }
        }
    }
}

/**
 * Display labels for the printing enums. The stored values stay the enum names, so a
 * printer configured in one language still resolves in another (NFR8).
 */
@StringRes
private fun PrinterLink.labelRes(): Int = when (this) {
    PrinterLink.TCP -> R.string.printer_link_tcp
    PrinterLink.BLUETOOTH -> R.string.printer_link_bluetooth
    PrinterLink.USB -> R.string.printer_link_usb
}

@StringRes
private fun PrinterLink.addressLabelRes(): Int = when (this) {
    PrinterLink.TCP -> R.string.printers_address_tcp
    PrinterLink.BLUETOOTH -> R.string.printers_address_bluetooth
    PrinterLink.USB -> R.string.printers_address_usb
}

@StringRes
private fun PrinterRole.labelRes(): Int = when (this) {
    PrinterRole.KITCHEN -> R.string.printer_role_kitchen
    PrinterRole.BAR -> R.string.printer_role_bar
    PrinterRole.RECEIPT -> R.string.printer_role_receipt
}

private const val SEPARATOR = ", "
private const val PAPER_58 = 58
private const val PAPER_80 = 80
