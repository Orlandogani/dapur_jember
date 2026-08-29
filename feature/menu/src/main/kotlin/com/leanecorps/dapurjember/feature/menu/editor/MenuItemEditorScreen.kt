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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton

@Composable
fun MenuItemEditorScreen(
    onDone: () -> Unit,
    viewModel: MenuItemEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (state.isNew) "New item" else "Edit item") })
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
            onSave = viewModel::save,
            onDelete = viewModel::delete,
        )
    }
}

@Composable
private fun EditorBody(
    state: MenuItemEditorState,
    modifier: Modifier,
    onEdit: ((MenuItemDraft) -> MenuItemDraft) -> Unit,
    onEditVariant: (String, (VariantDraft) -> VariantDraft) -> Unit,
    onAddVariant: () -> Unit,
    onRemoveVariant: (String) -> Unit,
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
            label = { Text("Item name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Category", style = MaterialTheme.typography.labelLarge)
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
            Text("Add a category first.", color = MaterialTheme.colorScheme.error)
        }

        ToggleRow("Available", draft.available) { v -> onEdit { it.copy(available = v) } }
        ToggleRow("Tax exempt", draft.taxExempt) { v -> onEdit { it.copy(taxExempt = v) } }

        Text("Variants", style = MaterialTheme.typography.labelLarge)
        draft.variants.forEach { variant ->
            VariantRow(
                variant = variant,
                minorUnits = state.currencyMinorUnits,
                canRemove = draft.variants.size > 1,
                onName = { v -> onEditVariant(variant.id) { it.copy(name = v) } },
                onPrice = { v -> onEditVariant(variant.id) { it.copy(priceText = v) } },
                onRemove = { onRemoveVariant(variant.id) },
            )
        }
        PosOutlinedButton(text = "Add variant", onClick = onAddVariant)

        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!state.isNew) {
                PosOutlinedButton(text = "Delete", onClick = onDelete, modifier = Modifier.weight(1f))
            }
            PosButton(
                text = "Save",
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
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = variant.priceText,
            onValueChange = onPrice,
            label = { Text("Price") },
            isError = variant.priceMinor(minorUnits) == null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        if (canRemove) {
            PosOutlinedButton(text = "×", onClick = onRemove)
        }
    }
}

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
