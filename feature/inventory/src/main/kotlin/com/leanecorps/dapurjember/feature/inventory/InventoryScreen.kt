@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.leanecorps.dapurjember.feature.inventory

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton

@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Inventory") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PosOutlinedButton(text = "Back", onClick = onBack, modifier = Modifier.weight(1f))
                PosButton(text = "Add ingredient", onClick = viewModel::startAdd, modifier = Modifier.weight(1f))
            }
            if (state.ingredients.isEmpty() && !state.loading) {
                Text(
                    "No ingredients yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.ingredients, key = { it.id }) { row ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(row.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (row.lowStock) "${row.stockLabel}  · LOW" else row.stockLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (row.lowStock) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PosOutlinedButton(text = "Edit", onClick = { viewModel.startEdit(row.id) })
                                PosOutlinedButton(text = "Adjust", onClick = { viewModel.startAdjust(row.id) })
                            }
                        }
                    }
                }
            }
        }
    }

    state.editor?.let { editor ->
        IngredientEditorDialog(
            editor = editor,
            onChange = viewModel::editIngredient,
            onDismiss = viewModel::closeEditor,
            onDelete = { editor.id?.let(viewModel::deleteIngredient) },
            onSave = viewModel::saveIngredient,
        )
    }
    state.adjust?.let { adjust ->
        AdjustStockDialog(
            adjust = adjust,
            onChange = viewModel::editAdjust,
            onDismiss = viewModel::closeAdjust,
            onApply = viewModel::applyAdjust,
        )
    }
}

@Composable
private fun IngredientEditorDialog(
    editor: IngredientDraft,
    onChange: ((IngredientDraft) -> IngredientDraft) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
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
        title = { Text(if (editor.isNew) "New ingredient" else "Edit ingredient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { v -> onChange { it.copy(name = v) } },
                    label = { Text("Name") },
                    singleLine = true,
                )
                Text("Base unit", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InventoryViewModel.BASE_UNITS.forEach { unit ->
                        FilterChip(
                            selected = unit == editor.baseUnit,
                            onClick = { onChange { it.copy(baseUnit = unit) } },
                            label = { Text(unit.name) },
                        )
                    }
                }
                OutlinedTextField(
                    value = editor.purchaseUnit,
                    onValueChange = { v -> onChange { it.copy(purchaseUnit = v) } },
                    label = { Text("Purchase unit (e.g. sack, box)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = editor.purchaseToBaseFactorText,
                    onValueChange = { v -> onChange { it.copy(purchaseToBaseFactorText = v) } },
                    label = { Text("1 purchase unit = ? base units") },
                    isError = editor.factor == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = editor.lowStockThresholdText,
                    onValueChange = { v -> onChange { it.copy(lowStockThresholdText = v) } },
                    label = { Text("Low-stock threshold (base units)") },
                    isError = editor.threshold == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
    )
}

@Composable
private fun AdjustStockDialog(
    adjust: AdjustDraft,
    onChange: ((AdjustDraft) -> AdjustDraft) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { PosButton(text = "Apply", onClick = onApply, enabled = adjust.canApply) },
        dismissButton = { PosOutlinedButton(text = "Cancel", onClick = onDismiss) },
        title = { Text("Adjust ${adjust.ingredientName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Reason", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InventoryViewModel.ADJUST_REASONS.forEach { reason ->
                        FilterChip(
                            selected = reason == adjust.reason,
                            onClick = { onChange { it.copy(reason = reason) } },
                            label = { Text(reason.name.replace('_', ' ')) },
                        )
                    }
                }
                OutlinedTextField(
                    value = adjust.qtyText,
                    onValueChange = { v -> onChange { it.copy(qtyText = v) } },
                    label = { Text("Quantity in ${adjust.baseUnit.name.lowercase()} (- to remove)") },
                    isError = adjust.qty == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (adjust.isPurchase) {
                    OutlinedTextField(
                        value = adjust.unitCostText,
                        onValueChange = { v -> onChange { it.copy(unitCostText = v) } },
                        label = { Text("Cost per ${adjust.baseUnit.name.lowercase()}") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
            }
        },
    )
}
