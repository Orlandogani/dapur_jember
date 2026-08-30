package com.leanecorps.dapurjember.feature.order

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
        confirmButton = { PosButton(text = "Void line", onClick = onConfirm, enabled = action.canVoid) },
        dismissButton = { PosOutlinedButton(text = "Cancel", onClick = onDismiss) },
        title = { Text("Void ${action.lineName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (action.sent) {
                    Text(
                        "This line was already sent to the kitchen. It stays on the bill as voided " +
                            "and appears in the audit report.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text("Reason", style = MaterialTheme.typography.labelLarge)
                VoidReason.entries.forEach { reason ->
                    FilterChip(
                        selected = reason == action.reason,
                        onClick = { onChange { it.copy(reason = reason) } },
                        label = { Text(reason.label) },
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
                OutlinedTextField(
                    value = action.note,
                    onValueChange = { v -> onChange { it.copy(note = v) } },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                )
                if (action.needsStepUp) {
                    StepUpPinField(
                        pin = action.pin,
                        error = action.error,
                        onPin = { v -> onChange { it.copy(pin = v, error = null) } },
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
        confirmButton = { PosButton(text = "Apply", onClick = onConfirm, enabled = draft.canApply) },
        dismissButton = { PosOutlinedButton(text = "Cancel", onClick = onDismiss) },
        title = { Text("Discount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiscountKind.entries.forEach { kind ->
                        FilterChip(
                            selected = kind == draft.kind,
                            onClick = { onChange { it.copy(kind = kind) } },
                            label = { Text(if (kind == DiscountKind.PERCENT) "Percent" else "Fixed") },
                        )
                    }
                }
                OutlinedTextField(
                    value = draft.valueText,
                    onValueChange = { v -> onChange { it.copy(valueText = v) } },
                    label = { Text(if (draft.kind == DiscountKind.PERCENT) "Percent (%)" else "Amount") },
                    isError = draft.valueText.isNotBlank() && draft.value == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = draft.reason,
                    onValueChange = { v -> onChange { it.copy(reason = v) } },
                    label = { Text("Reason (required)") },
                    singleLine = true,
                )
                if (draft.needsStepUp) {
                    StepUpPinField(
                        pin = draft.pin,
                        error = draft.error,
                        onPin = { v -> onChange { it.copy(pin = v, error = null) } },
                    )
                }
            }
        },
    )
}

/** The step-up field (S32): a manager authorises without anyone being logged out (FR-A3). */
@Composable
private fun StepUpPinField(pin: String, error: String?, onPin: (String) -> Unit) {
    Column {
        Text("Manager authorisation required", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = pin,
            onValueChange = { onPin(it.filter(Char::isDigit)) },
            label = { Text("Manager PIN") },
            singleLine = true,
            isError = error != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
