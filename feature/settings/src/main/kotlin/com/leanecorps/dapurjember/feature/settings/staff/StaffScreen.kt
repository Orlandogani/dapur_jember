@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.leanecorps.dapurjember.feature.settings.staff

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.domain.auth.Staff
import com.leanecorps.dapurjember.core.domain.auth.StaffRole
import com.leanecorps.dapurjember.feature.settings.R

@Composable
fun StaffScreen(
    onBack: () -> Unit,
    viewModel: StaffViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val messageText = state.message?.let { stringResource(it.labelRes()) }
    LaunchedEffect(messageText) {
        messageText?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.staff_title)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PosOutlinedButton(
                    text = stringResource(R.string.staff_action_back),
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                PosButton(
                    text = stringResource(R.string.staff_action_add),
                    onClick = viewModel::startAdd,
                    enabled = state.canManage,
                    modifier = Modifier.weight(1f),
                )
            }

            if (!state.canManage && !state.loading) {
                Text(
                    stringResource(R.string.staff_no_permission),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                items(state.staff, key = { it.id }) { staff ->
                    StaffRow(
                        staff = staff,
                        isSelf = staff.id == state.currentStaffId,
                        canManage = state.canManage,
                        onEdit = { viewModel.startEdit(staff) },
                        onToggleActive = { viewModel.setActive(staff, !staff.active) },
                    )
                }
            }
        }
    }

    state.editor?.let { draft ->
        StaffEditorDialog(
            draft = draft,
            onChange = viewModel::edit,
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::save,
        )
    }
}

@Composable
private fun StaffRow(
    staff: Staff,
    isSelf: Boolean,
    canManage: Boolean,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                if (isSelf) stringResource(R.string.staff_name_you, staff.name) else staff.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(staff.role.labelRes()).let {
                    if (staff.active) it else stringResource(R.string.staff_role_line_deactivated, it)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (staff.active) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            if (canManage) {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosOutlinedButton(text = stringResource(R.string.staff_action_edit), onClick = onEdit)
                    PosOutlinedButton(
                        text = stringResource(
                            if (staff.active) R.string.staff_action_deactivate else R.string.staff_action_reactivate,
                        ),
                        onClick = onToggleActive,
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffEditorDialog(
    draft: StaffDraft,
    onChange: ((StaffDraft) -> StaffDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PosButton(text = stringResource(R.string.staff_action_save), onClick = onSave, enabled = draft.canSave)
        },
        dismissButton = { PosOutlinedButton(text = stringResource(R.string.staff_action_cancel), onClick = onDismiss) },
        title = {
            Text(
                if (draft.isNew) {
                    stringResource(R.string.staff_editor_new)
                } else {
                    stringResource(R.string.staff_editor_edit, draft.name)
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { v -> onChange { it.copy(name = v) } },
                    label = { Text(stringResource(R.string.staff_name_label)) },
                    singleLine = true,
                )
                Text(stringResource(R.string.staff_role_heading), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StaffRole.entries.forEach { role ->
                        FilterChip(
                            selected = role == draft.role,
                            onClick = { onChange { it.copy(role = role) } },
                            label = { Text(stringResource(role.labelRes())) },
                        )
                    }
                }
                OutlinedTextField(
                    value = draft.pin,
                    onValueChange = { v -> onChange { it.copy(pin = v.filter(Char::isDigit).take(6)) } },
                    label = {
                        Text(
                            stringResource(
                                if (draft.isNew) R.string.staff_pin_label_new else R.string.staff_pin_label_edit,
                            ),
                        )
                    },
                    isError = !draft.pinValid,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            }
        },
    )
}

/** Display labels; the persisted role stays the enum name so it survives translation (NFR8). */
@StringRes
private fun StaffRole.labelRes(): Int = when (this) {
    StaffRole.OWNER -> R.string.staff_role_owner
    StaffRole.MANAGER -> R.string.staff_role_manager
    StaffRole.CASHIER -> R.string.staff_role_cashier
    StaffRole.WAITER -> R.string.staff_role_waiter
}

@StringRes
private fun StaffMessage.labelRes(): Int = when (this) {
    StaffMessage.CANNOT_DEACTIVATE_SELF -> R.string.staff_cannot_deactivate_self
}
