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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton

@Composable
fun ModifierGroupsScreen(
    onBack: () -> Unit,
    viewModel: ModifierGroupsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Modifier groups") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PosOutlinedButton(text = "Back", onClick = onBack, modifier = Modifier.weight(1f))
                PosButton(text = "Add group", onClick = viewModel::startAdd, modifier = Modifier.weight(1f))
            }
            if (state.groups.isEmpty() && !state.loading) {
                Text(
                    "No modifier groups yet. Create one (e.g. \"Spice level\") and attach it to items.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.groups, key = { it.id }) { group ->
                    Card(Modifier.fillMaxWidth().clickable { viewModel.startEdit(group.id) }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(group.name, style = MaterialTheme.typography.titleMedium)
                            Text(group.summary, style = MaterialTheme.typography.bodySmall)
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
        confirmButton = { PosButton(text = "Save", onClick = onSave, enabled = editor.canSave) },
        dismissButton = {
            if (editor.isNew) {
                PosOutlinedButton(text = "Cancel", onClick = onDismiss)
            } else {
                PosOutlinedButton(text = "Delete", onClick = onDelete)
            }
        },
        title = { Text(if (editor.isNew) "New modifier group" else "Edit modifier group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { v -> onChange { it.copy(name = v) } },
                    label = { Text("Group name") },
                    singleLine = true,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Required")
                    Switch(checked = editor.required, onCheckedChange = { v -> onChange { it.copy(required = v) } })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editor.minSelectText,
                        onValueChange = { v -> onChange { it.copy(minSelectText = v) } },
                        label = { Text("Min") },
                        isError = editor.minSelect == null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = editor.maxSelectText,
                        onValueChange = { v -> onChange { it.copy(maxSelectText = v) } },
                        label = { Text("Max (0 = any)") },
                        isError = editor.maxSelect == null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text("Options", style = MaterialTheme.typography.labelLarge)
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
                PosOutlinedButton(text = "Add option", onClick = onAddModifier)
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
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.weight(1.4f),
        )
        OutlinedTextField(
            value = row.priceDeltaText,
            onValueChange = onPrice,
            label = { Text("+/-") },
            isError = row.priceDeltaMinorFor(0) == null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        Switch(checked = row.defaultSelected, onCheckedChange = onDefault)
        if (canRemove) PosOutlinedButton(text = "×", onClick = onRemove)
    }
}
