@file:OptIn(ExperimentalMaterial3Api::class)

package com.leanecorps.dapurjember.feature.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton

@Composable
fun MenuScreen(
    onEditItem: (itemId: String?) -> Unit,
    onImportCsv: () -> Unit,
    onModifierGroups: () -> Unit,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MenuScreen(
        state = state,
        onToggleAvailability = viewModel::setAvailability,
        onAddCategory = viewModel::addCategory,
        onEditItem = onEditItem,
        onImportCsv = onImportCsv,
        onModifierGroups = onModifierGroups,
    )
}

@Composable
internal fun MenuScreen(
    state: MenuUiState,
    onToggleAvailability: (itemId: String, available: Boolean) -> Unit,
    onAddCategory: (String) -> Unit,
    onEditItem: (itemId: String?) -> Unit,
    onImportCsv: () -> Unit,
    onModifierGroups: () -> Unit,
) {
    var showCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_title)) },
                actions = {
                    // Every editing entry point is hidden outright rather than disabled: a
                    // waiter has no use for a row of greyed-out buttons mid-service.
                    if (state.canManage) {
                        TextButton(onClick = onModifierGroups) {
                            Text(stringResource(R.string.menu_action_modifiers))
                        }
                        TextButton(onClick = onImportCsv) { Text(stringResource(R.string.menu_action_import_csv)) }
                        TextButton(onClick = { showCategoryDialog = true }) {
                            Text(stringResource(R.string.menu_action_add_category))
                        }
                        TextButton(onClick = { onEditItem(null) }) {
                            Text(stringResource(R.string.menu_action_add_item))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

                state.isEmpty -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(stringResource(R.string.menu_empty), style = MaterialTheme.typography.bodyLarge)
                }

                else -> LazyColumn(Modifier.fillMaxSize()) {
                    if (!state.canManage) {
                        item(key = "no_permission") {
                            Text(
                                text = stringResource(R.string.menu_no_permission),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                    state.sections.forEach { section ->
                        item(key = "h_${section.id}") {
                            Text(
                                text = section.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        items(section.items, key = { it.id }) { item ->
                            MenuItemRow(
                                item = item,
                                onToggle = { onToggleAvailability(item.id, it) },
                                // The sold-out switch stays live for everyone (FR-M2); only
                                // opening the editor needs MANAGE_MENU.
                                onClick = if (state.canManage) {
                                    { onEditItem(item.id) }
                                } else {
                                    null
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showCategoryDialog = false },
            onConfirm = { name ->
                onAddCategory(name)
                showCategoryDialog = false
            },
        )
    }
}

@Composable
private fun MenuItemRow(item: MenuItemUi, onToggle: (Boolean) -> Unit, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick == null) it else it.clickable(onClick = onClick) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (item.available) TextDecoration.None else TextDecoration.LineThrough,
        )
        Switch(checked = item.available, onCheckedChange = onToggle)
    }
}

@Composable
private fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PosButton(
                text = stringResource(R.string.menu_action_add),
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            )
        },
        dismissButton = { PosOutlinedButton(text = stringResource(R.string.menu_action_cancel), onClick = onDismiss) },
        title = { Text(stringResource(R.string.menu_category_dialog_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.menu_category_name_label)) },
                singleLine = true,
            )
        },
    )
}
