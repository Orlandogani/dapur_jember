package com.leanecorps.dapurjember.feature.shift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.MoneyText
import com.leanecorps.dapurjember.core.designsystem.component.PosButton

@Composable
fun ShiftScreen(
    onDone: () -> Unit,
    viewModel: ShiftViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var enteredWithShiftOpen by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(state.loading) {
        if (!state.loading && enteredWithShiftOpen == null) enteredWithShiftOpen = state.shiftOpen
    }
    LaunchedEffect(state.shiftOpen, state.closed) {
        val entered = enteredWithShiftOpen ?: return@LaunchedEffect
        if ((!entered && state.shiftOpen) || state.closed) onDone()
    }

    ShiftScreen(
        state = state,
        onOpen = viewModel::openShift,
        onPreviewClose = viewModel::previewClose,
        onConfirmClose = viewModel::confirmClose,
    )
}

@Composable
@Suppress("LongParameterList")
internal fun ShiftScreen(
    state: ShiftUiState,
    onOpen: (Long) -> Unit,
    onPreviewClose: (Long) -> Unit,
    onConfirmClose: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!state.shiftOpen) {
            OpenShiftForm(onOpen = onOpen)
        } else {
            CloseShiftForm(state = state, onPreviewClose = onPreviewClose, onConfirmClose = onConfirmClose)
        }
    }
}

@Composable
private fun OpenShiftForm(onOpen: (Long) -> Unit) {
    var float by remember { mutableStateOf("") }
    Text(stringResource(R.string.shift_open_title), style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = float,
        onValueChange = { float = it.filter(Char::isDigit) },
        label = { Text(stringResource(R.string.shift_opening_float_label)) },
    )
    PosButton(
        text = stringResource(R.string.shift_action_open),
        onClick = { onOpen(float.toLongOrNull() ?: 0L) },
        enabled = float.isNotEmpty(),
    )
}

@Composable
private fun CloseShiftForm(
    state: ShiftUiState,
    onPreviewClose: (Long) -> Unit,
    onConfirmClose: () -> Unit,
) {
    var counted by remember { mutableStateOf("") }
    Text(stringResource(R.string.shift_close_title), style = MaterialTheme.typography.titleLarge)

    if (state.blockedByOrders > 0) {
        Text(
            pluralStringResource(
                R.plurals.shift_blocked_by_orders,
                state.blockedByOrders,
                state.blockedByOrders,
            ),
            color = MaterialTheme.colorScheme.error,
        )
    }

    val preview = state.closePreview
    if (preview == null) {
        OutlinedTextField(
            value = counted,
            onValueChange = { counted = it.filter(Char::isDigit) },
            label = { Text(stringResource(R.string.shift_counted_cash_label)) },
        )
        PosButton(
            text = stringResource(R.string.shift_action_reveal_z_report),
            onClick = { onPreviewClose(counted.toLongOrNull() ?: 0L) },
            enabled = counted.isNotEmpty(),
        )
    } else {
        ZRow(stringResource(R.string.shift_z_counted), preview.countedCash.minor)
        ZRow(stringResource(R.string.shift_z_expected), preview.expectedCash.minor)
        ZRow(stringResource(R.string.shift_z_variance), preview.variance.minor, emphasise = true)
        PosButton(text = stringResource(R.string.shift_action_confirm_close), onClick = onConfirmClose)
    }
}

@Composable
private fun ZRow(label: String, amountMinor: Long, emphasise: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        MoneyText(
            amountMinor.toString(),
            style = if (emphasise) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
    }
}
