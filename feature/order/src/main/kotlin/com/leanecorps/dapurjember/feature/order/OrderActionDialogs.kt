package com.leanecorps.dapurjember.feature.order

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.domain.order.DiscountKind
import com.leanecorps.dapurjember.core.domain.order.VoidReason

@Composable
internal fun LineActionDialog(
    action: LineActionUiState,
    onChange: ((LineActionUiState) -> LineActionUiState) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PosButton(text = stringResource(R.string.void_action), onClick = onConfirm, enabled = action.canVoid)
        },
        dismissButton = { PosOutlinedButton(text = stringResource(R.string.action_cancel), onClick = onDismiss) },
        title = { Text(stringResource(R.string.void_title, action.lineName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (action.sent) {
                    Text(
                        stringResource(R.string.void_already_sent),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(stringResource(R.string.void_reason_heading), style = MaterialTheme.typography.labelLarge)
                VoidReason.entries.forEach { reason ->
                    FilterChip(
                        selected = reason == action.reason,
                        onClick = { onChange { it.copy(reason = reason) } },
                        label = { Text(stringResource(reason.labelRes())) },
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
                OutlinedTextField(
                    value = action.note,
                    onValueChange = { v -> onChange { it.copy(note = v) } },
                    label = { Text(stringResource(R.string.void_note_label)) },
                    singleLine = true,
                )
                if (action.needsStepUp) {
                    StepUpPinField(
                        pin = action.pin,
                        rejected = action.pinRejected,
                        onPin = { v -> onChange { it.copy(pin = v, pinRejected = false) } },
                    )
                }
            }
        },
    )
}

@Composable
internal fun DiscountDialog(
    draft: DiscountUiState,
    onChange: ((DiscountUiState) -> DiscountUiState) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PosButton(
                text = stringResource(R.string.discount_action_apply),
                onClick = onConfirm,
                enabled = draft.canApply,
            )
        },
        dismissButton = { PosOutlinedButton(text = stringResource(R.string.action_cancel), onClick = onDismiss) },
        title = { Text(stringResource(R.string.discount_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiscountKind.entries.forEach { kind ->
                        FilterChip(
                            selected = kind == draft.kind,
                            onClick = { onChange { it.copy(kind = kind) } },
                            label = {
                                Text(
                                    stringResource(
                                        if (kind == DiscountKind.PERCENT) {
                                            R.string.discount_kind_percent
                                        } else {
                                            R.string.discount_kind_fixed
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = draft.valueText,
                    onValueChange = { v -> onChange { it.copy(valueText = v) } },
                    label = {
                        Text(
                            stringResource(
                                if (draft.kind == DiscountKind.PERCENT) {
                                    R.string.discount_value_percent_label
                                } else {
                                    R.string.discount_value_fixed_label
                                },
                            ),
                        )
                    },
                    isError = draft.valueText.isNotBlank() && draft.value == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = draft.reason,
                    onValueChange = { v -> onChange { it.copy(reason = v) } },
                    label = { Text(stringResource(R.string.discount_reason_label)) },
                    singleLine = true,
                )
                if (draft.needsStepUp) {
                    StepUpPinField(
                        pin = draft.pin,
                        rejected = draft.pinRejected,
                        onPin = { v -> onChange { it.copy(pin = v, pinRejected = false) } },
                    )
                }
            }
        },
    )
}

/** The step-up field (S32): a manager authorises without anyone being logged out (FR-A3). */
@Composable
private fun StepUpPinField(pin: String, rejected: Boolean, onPin: (String) -> Unit) {
    Column {
        Text(stringResource(R.string.stepup_heading), style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = pin,
            onValueChange = { onPin(it.filter(Char::isDigit)) },
            label = { Text(stringResource(R.string.stepup_pin_label)) },
            singleLine = true,
            isError = rejected,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        if (rejected) {
            Text(
                stringResource(R.string.stepup_rejected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Display label for a void reason. Kept in the UI so the stored value stays language-neutral. */
@StringRes
internal fun VoidReason.labelRes(): Int = when (this) {
    VoidReason.WRONG_ORDER -> R.string.void_reason_wrong_order
    VoidReason.CUSTOMER_CHANGED_MIND -> R.string.void_reason_customer_changed_mind
    VoidReason.QUALITY_ISSUE -> R.string.void_reason_quality_issue
    VoidReason.OUT_OF_STOCK -> R.string.void_reason_out_of_stock
    VoidReason.STAFF_ERROR -> R.string.void_reason_staff_error
    VoidReason.OTHER -> R.string.void_reason_other
}
