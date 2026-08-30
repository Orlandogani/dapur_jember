package com.leanecorps.dapurjember.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.designsystem.theme.PosTouchTarget

@Composable
fun PinLockScreen(
    onSignedIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }
    PinLockScreen(
        state = state,
        onSelectStaff = viewModel::selectStaff,
        onDigit = viewModel::appendDigit,
        onDelete = viewModel::deleteDigit,
        onSubmit = viewModel::submit,
    )
}

@Composable
@Suppress("LongParameterList")
internal fun PinLockScreen(
    state: AuthUiState,
    onSelectStaff: (String) -> Unit,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(
                if (state.firstRun) R.string.auth_title_set_owner_pin else R.string.auth_title_enter_pin,
            ),
            style = MaterialTheme.typography.titleLarge,
        )

        if (!state.firstRun) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.staff, key = { it.id }) { member ->
                    FilterChip(
                        selected = member.id == state.selectedStaffId,
                        onClick = { onSelectStaff(member.id) },
                        label = { Text(member.name) },
                    )
                }
            }
        }

        Text(
            text = "•".repeat(state.pin.length).padEnd(AuthUiState.PIN_MAX, '·'),
            style = MaterialTheme.typography.titleLarge,
            color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )

        Keypad(onDigit = onDigit, onDelete = onDelete)

        PosButton(
            text = stringResource(
                if (state.firstRun) R.string.auth_action_create_and_sign_in else R.string.auth_action_sign_in,
            ),
            onClick = onSubmit,
            enabled = state.canSubmit,
        )
    }
}

@Composable
private fun Keypad(onDigit: (Char) -> Unit, onDelete: () -> Unit) {
    val rows = listOf("123", "456", "789")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit -> KeyButton(digit.toString()) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(PosTouchTarget))
            KeyButton("0") { onDigit('0') }
            KeyButton("⌫", onDelete)
        }
    }
}

@Composable
private fun KeyButton(label: String, onClick: () -> Unit) {
    PosOutlinedButton(
        text = label,
        onClick = onClick,
        modifier = Modifier.size(width = PosTouchTarget * 1.4f, height = PosTouchTarget),
    )
}
