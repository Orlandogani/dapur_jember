@file:OptIn(ExperimentalMaterial3Api::class)

package com.leanecorps.dapurjember.feature.menu.modifiers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.feature.menu.R

@Composable
fun ModifierGroupsScreen(
    onBack: () -> Unit,
    viewModel: ModifierGroupsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.groups_title)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PosOutlinedButton(
                    text = stringResource(R.string.groups_action_back),
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                PosButton(
                    text = stringResource(R.string.groups_action_add),
                    onClick = viewModel::startAdd,
                    enabled = state.canManage,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!state.canManage && !state.loading) {
                Text(
                    stringResource(R.string.groups_no_permission),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            if (state.groups.isEmpty() && !state.loading) {
                Text(
                    stringResource(R.string.groups_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.groups, key = { it.id }) { group ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .let {
                                if (state.canManage) it.clickable { viewModel.startEdit(group.id) } else it
                            },
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(group.name, style = MaterialTheme.typography.titleMedium)
                            Text(group.summaryText(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    state.editor?.let { editor ->
        ModifierGroupEditorDialog(
            editor = editor,
            onChange = viewModel::edit,
            onEditModifier = viewModel::editModifier,
            onAddModifier = viewModel::addModifier,
            onRemoveModifier = viewModel::removeModifier,
            onDelete = {
                editor.id?.let(viewModel::delete)
                viewModel.closeEditor()
            },
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::save,
        )
    }
}

@Composable
private fun ModifierGroupEditorDialog(
    editor: ModifierGroupDraft,
    onChange: ((ModifierGroupDraft) -> ModifierGroupDraft) -> Unit,
    onEditModifier: (String, (ModifierRowDraft) -> ModifierRowDraft) -> Unit,
    onAddModifier: () -> Unit,
    onRemoveModifier: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PosButton(text = stringResource(R.string.groups_action_save), onClick = onSave, enabled = editor.canSave)
        },
        dismissButton = {
            if (editor.isNew) {
                PosOutlinedButton(text = stringResource(R.string.groups_action_cancel), onClick = onDismiss)
            } else {
                PosOutlinedButton(text = stringResource(R.string.groups_action_delete), onClick = onDelete)
            }
        },
        title = {
            Text(
                stringResource(
                    if (editor.isNew) R.string.groups_editor_new else R.string.groups_editor_edit,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { v -> onChange { it.copy(name = v) } },
                    label = { Text(stringResource(R.string.groups_name_label)) },
                    singleLine = true,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.groups_required))
                    Switch(checked = editor.required, onCheckedChange = { v -> onChange { it.copy(required = v) } })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editor.minSelectText,
                        onValueChange = { v -> onChange { it.copy(minSelectText = v) } },
                        label = { Text(stringResource(R.string.groups_min_label)) },
                        isError = editor.minSelect == null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = editor.maxSelectText,
                        onValueChange = { v -> onChange { it.copy(maxSelectText = v) } },
                        label = { Text(stringResource(R.string.groups_max_label)) },
                        isError = editor.maxSelect == null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(stringResource(R.string.groups_options_heading), style = MaterialTheme.typography.labelLarge)
                editor.modifiers.forEach { row ->
                    ModifierRow(
                        row = row,
                        canRemove = editor.modifiers.size > 1,
                        onName = { v -> onEditModifier(row.id) { it.copy(name = v) } },
                        onPrice = { v -> onEditModifier(row.id) { it.copy(priceDeltaText = v) } },
                        onDefault = { v -> onEditModifier(row.id) { it.copy(defaultSelected = v) } },
                        onRemove = { onRemoveModifier(row.id) },
                    )
                }
                PosOutlinedButton(text = stringResource(R.string.groups_action_add_option), onClick = onAddModifier)
            }
        },
    )
}

@Composable
private fun ModifierRow(
    row: ModifierRowDraft,
    canRemove: Boolean,
    onName: (String) -> Unit,
    onPrice: (String) -> Unit,
    onDefault: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = row.name,
            onValueChange = onName,
            label = { Text(stringResource(R.string.groups_option_name_label)) },
            singleLine = true,
            modifier = Modifier.weight(1.4f),
        )
        OutlinedTextField(
            value = row.priceDeltaText,
            onValueChange = onPrice,
            label = { Text(stringResource(R.string.groups_option_price_label)) },
            isError = row.priceDeltaMinorFor(0) == null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        Switch(checked = row.defaultSelected, onCheckedChange = onDefault)
        if (canRemove) PosOutlinedButton(text = stringResource(R.string.groups_action_remove), onClick = onRemove)
    }
}

/**
 * "3 options · required · choose 1–any". Built from the row's numbers rather than stored as
 * a sentence, so plural rules and word order stay the translator's business (NFR8).
 */
@Composable
private fun ModifierGroupRowUi.summaryText(): String {
    val options = pluralStringResource(R.plurals.groups_summary_options, optionCount, optionCount)
    val obligation = stringResource(
        if (required) R.string.groups_summary_required else R.string.groups_summary_optional,
    )
    val max = if (maxSelect == 0) stringResource(R.string.groups_summary_any) else maxSelect.toString()
    return stringResource(R.string.groups_summary, options, obligation, minSelect, max)
}
