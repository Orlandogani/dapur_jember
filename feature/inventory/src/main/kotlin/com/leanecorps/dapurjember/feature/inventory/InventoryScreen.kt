@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.leanecorps.dapurjember.feature.inventory

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.domain.inventory.StockReason

@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.inventory_title)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PosOutlinedButton(
                    text = stringResource(R.string.inventory_action_back),
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                PosButton(
                    text = stringResource(R.string.inventory_action_add),
                    onClick = viewModel::startAdd,
                    enabled = state.canAdjust,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!state.canAdjust && !state.loading) {
                Text(
                    stringResource(R.string.inventory_no_permission),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            if (state.ingredients.isEmpty() && !state.loading) {
                Text(
                    stringResource(R.string.inventory_empty),
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
                                if (row.lowStock) {
                                    stringResource(R.string.inventory_stock_low, row.stockLabel)
                                } else {
                                    row.stockLabel
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (row.lowStock) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PosOutlinedButton(
                                    text = stringResource(R.string.inventory_action_edit),
                                    onClick = { viewModel.startEdit(row.id) },
                                    enabled = state.canAdjust,
                                )
                                PosOutlinedButton(
                                    text = stringResource(R.string.inventory_action_adjust),
                                    onClick = { viewModel.startAdjust(row.id) },
                                    enabled = state.canAdjust,
                                )
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
        confirmButton = {
            PosButton(text = stringResource(R.string.inventory_action_save), onClick = onSave, enabled = editor.canSave)
        },
        dismissButton = {
            if (editor.isNew) {
                PosOutlinedButton(text = stringResource(R.string.inventory_action_cancel), onClick = onDismiss)
            } else {
                PosOutlinedButton(text = stringResource(R.string.inventory_action_delete), onClick = onDelete)
            }
        },
        title = {
            Text(
                stringResource(
                    if (editor.isNew) R.string.inventory_editor_new else R.string.inventory_editor_edit,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { v -> onChange { it.copy(name = v) } },
                    label = { Text(stringResource(R.string.inventory_name_label)) },
                    singleLine = true,
                )
                Text(stringResource(R.string.inventory_base_unit_heading), style = MaterialTheme.typography.labelLarge)
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
                    label = { Text(stringResource(R.string.inventory_purchase_unit_label)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = editor.purchaseToBaseFactorText,
                    onValueChange = { v -> onChange { it.copy(purchaseToBaseFactorText = v) } },
                    label = { Text(stringResource(R.string.inventory_factor_label)) },
                    isError = editor.factor == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = editor.lowStockThresholdText,
                    onValueChange = { v -> onChange { it.copy(lowStockThresholdText = v) } },
                    label = { Text(stringResource(R.string.inventory_threshold_label)) },
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
        confirmButton = {
            PosButton(
                text = stringResource(R.string.inventory_action_apply),
                onClick = onApply,
                enabled = adjust.canApply,
            )
        },
        dismissButton = {
            PosOutlinedButton(
                text = stringResource(R.string.inventory_action_cancel),
                onClick = onDismiss,
            )
        },
        title = { Text(stringResource(R.string.inventory_adjust_title, adjust.ingredientName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.inventory_adjust_reason_heading),
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InventoryViewModel.ADJUST_REASONS.forEach { reason ->
                        FilterChip(
                            selected = reason == adjust.reason,
                            onClick = { onChange { it.copy(reason = reason) } },
                            label = { Text(stringResource(reason.labelRes())) },
                        )
                    }
                }
                OutlinedTextField(
                    value = adjust.qtyText,
                    onValueChange = { v -> onChange { it.copy(qtyText = v) } },
                    label = {
                        Text(
                            stringResource(R.string.inventory_adjust_qty_label, adjust.baseUnit.name.lowercase()),
                        )
                    },
                    isError = adjust.qty == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (adjust.isPurchase) {
                    OutlinedTextField(
                        value = adjust.unitCostText,
                        onValueChange = { v -> onChange { it.copy(unitCostText = v) } },
                        label = {
                            Text(
                                stringResource(R.string.inventory_adjust_cost_label, adjust.baseUnit.name.lowercase()),
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
            }
        },
    )
}

/** Display label for a stock reason; the stored value stays the language-neutral enum name. */
@StringRes
internal fun StockReason.labelRes(): Int = when (this) {
    StockReason.PURCHASE -> R.string.stock_reason_purchase
    StockReason.WASTE -> R.string.stock_reason_waste
    StockReason.SPOILAGE -> R.string.stock_reason_spoilage
    StockReason.STAFF_MEAL -> R.string.stock_reason_staff_meal
    StockReason.COUNT_CORRECTION, StockReason.SALE, StockReason.OPENING ->
        R.string.stock_reason_count_correction
}
