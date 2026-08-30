@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.leanecorps.dapurjember.feature.menu.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.feature.menu.R
import java.util.Locale

@Composable
fun MenuItemEditorScreen(
    onDone: () -> Unit,
    viewModel: MenuItemEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isNew) R.string.editor_title_new else R.string.editor_title_edit))
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        EditorBody(
            state = state,
            modifier = Modifier.padding(padding),
            onEdit = viewModel::edit,
            onEditVariant = viewModel::editVariant,
            onAddVariant = viewModel::addVariant,
            onRemoveVariant = viewModel::removeVariant,
            onToggleModifierGroup = viewModel::toggleModifierGroup,
            onOpenRecipe = viewModel::openRecipe,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
        )
    }

    state.recipe?.let { recipe ->
        RecipeDialog(
            recipe = recipe,
            ingredients = state.ingredients,
            onEditRow = viewModel::editRecipeRow,
            onAddRow = viewModel::addRecipeRow,
            onRemoveRow = viewModel::removeRecipeRow,
            onDismiss = viewModel::closeRecipe,
            onSave = viewModel::saveRecipe,
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun RecipeDialog(
    recipe: RecipeEditorUi,
    ingredients: List<IngredientOption>,
    onEditRow: (Int, (RecipeRowDraft) -> RecipeRowDraft) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PosButton(text = stringResource(R.string.recipe_action_save), onClick = onSave, enabled = recipe.canSave)
        },
        dismissButton = {
            PosOutlinedButton(
                text = stringResource(R.string.recipe_action_cancel),
                onClick = onDismiss,
            )
        },
        title = { Text(stringResource(R.string.recipe_dialog_title, recipe.variantName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ingredients.isEmpty()) {
                    Text(stringResource(R.string.recipe_no_ingredients), color = MaterialTheme.colorScheme.error)
                }
                Text(
                    stringResource(R.string.recipe_cost_so_far, recipe.costMinor.toString()),
                    style = MaterialTheme.typography.labelLarge,
                )
                recipe.rows.forEachIndexed { index, row ->
                    Column {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ingredients.forEach { option ->
                                FilterChip(
                                    selected = option.id == row.ingredientId,
                                    onClick = { onEditRow(index) { it.copy(ingredientId = option.id) } },
                                    label = { Text(option.name) },
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = row.qtyText,
                                onValueChange = { v -> onEditRow(index) { it.copy(qtyText = v) } },
                                label = {
                                    val unit = ingredients.firstOrNull { it.id == row.ingredientId }?.baseUnit
                                        ?: stringResource(R.string.recipe_qty_unit_fallback)
                                    Text(stringResource(R.string.recipe_qty_label, unit))
                                },
                                isError = row.qty == null,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                            PosOutlinedButton(
                                text = stringResource(R.string.editor_action_remove),
                                onClick = { onRemoveRow(index) },
                            )
                        }
                    }
                }
                PosOutlinedButton(text = stringResource(R.string.recipe_action_add_ingredient), onClick = onAddRow)
            }
        },
    )
}

@Composable
@Suppress("LongParameterList")
private fun EditorBody(
    state: MenuItemEditorState,
    modifier: Modifier,
    onEdit: ((MenuItemDraft) -> MenuItemDraft) -> Unit,
    onEditVariant: (String, (VariantDraft) -> VariantDraft) -> Unit,
    onAddVariant: () -> Unit,
    onRemoveVariant: (String) -> Unit,
    onToggleModifierGroup: (String) -> Unit,
    onOpenRecipe: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val draft = state.draft
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = { v -> onEdit { it.copy(name = v) } },
            label = { Text(stringResource(R.string.editor_item_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.editor_category_heading), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.categories.forEach { option ->
                FilterChip(
                    selected = option.id == draft.categoryId,
                    onClick = { onEdit { it.copy(categoryId = option.id) } },
                    label = { Text(option.name) },
                )
            }
        }
        if (state.categories.isEmpty()) {
            Text(stringResource(R.string.editor_no_categories), color = MaterialTheme.colorScheme.error)
        }

        ToggleRow(stringResource(R.string.editor_available), draft.available) { v -> onEdit { it.copy(available = v) } }
        ToggleRow(
            stringResource(R.string.editor_tax_exempt),
            draft.taxExempt,
        ) { v -> onEdit { it.copy(taxExempt = v) } }

        Text(stringResource(R.string.editor_variants_heading), style = MaterialTheme.typography.labelLarge)
        draft.variants.forEach { variant ->
            VariantRow(
                variant = variant,
                minorUnits = state.currencyMinorUnits,
                canRemove = draft.variants.size > 1,
                onName = { v -> onEditVariant(variant.id) { it.copy(name = v) } },
                onPrice = { v -> onEditVariant(variant.id) { it.copy(priceText = v) } },
                onRemove = { onRemoveVariant(variant.id) },
            )
            // Recipes attach to the variant (FR-I2/FR-M4) and only exist once the item is saved.
            if (!state.isNew) {
                val margin = state.marginFor(variant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosOutlinedButton(
                        text = stringResource(R.string.recipe_action_open),
                        onClick = { onOpenRecipe(variant.id) },
                    )
                    Text(
                        text = margin.marginPercent?.let {
                            stringResource(R.string.recipe_margin, margin.costMinor.toString(), formatPercent(it))
                        } ?: stringResource(R.string.recipe_none),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }
        PosOutlinedButton(text = stringResource(R.string.editor_action_add_variant), onClick = onAddVariant)

        if (state.modifierGroups.isNotEmpty()) {
            Text(stringResource(R.string.editor_modifier_groups_heading), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.modifierGroups.forEach { group ->
                    FilterChip(
                        selected = group.id in draft.modifierGroupIds,
                        onClick = { onToggleModifierGroup(group.id) },
                        label = { Text(group.name) },
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!state.isNew) {
                PosOutlinedButton(
                    text = stringResource(R.string.editor_action_delete),
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                )
            }
            PosButton(
                text = stringResource(R.string.editor_action_save),
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VariantRow(
    variant: VariantDraft,
    minorUnits: Int,
    canRemove: Boolean,
    onName: (String) -> Unit,
    onPrice: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = variant.name,
            onValueChange = onName,
            label = { Text(stringResource(R.string.editor_variant_name_label)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = variant.priceText,
            onValueChange = onPrice,
            label = { Text(stringResource(R.string.editor_variant_price_label)) },
            isError = variant.priceMinor(minorUnits) == null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        if (canRemove) {
            PosOutlinedButton(text = stringResource(R.string.editor_action_remove), onClick = onRemove)
        }
    }
}

/** One decimal place, locale-independent so the number reads the same on every device. */
private fun formatPercent(value: Double): String = String.format(Locale.ROOT, "%.1f%%", value)

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
