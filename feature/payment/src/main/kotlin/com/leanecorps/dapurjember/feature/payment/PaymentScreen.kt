@file:OptIn(ExperimentalLayoutApi::class)

package com.leanecorps.dapurjember.feature.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.MoneyText
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.designsystem.component.SecureScreen
import com.leanecorps.dapurjember.core.domain.order.PaymentMethod

@Composable
fun PaymentScreen(
    onSettled: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SecureScreen()
    PaymentScreen(
        state = state,
        onPay = viewModel::pay,
        onSplit = viewModel::startSplit,
        onReprint = viewModel::reprintReceipt,
        onDone = onSettled,
    )
    state.split?.let { split ->
        SplitBillDialog(
            state = state,
            split = split,
            onMode = viewModel::setSplitMode,
            onWays = viewModel::setSplitWays,
            onCycleLine = viewModel::cycleLineAssignment,
            onPayPart = viewModel::paySplitPart,
            onDismiss = viewModel::closeSplit,
        )
    }
}

@Composable
internal fun PaymentScreen(
    state: PaymentUiState,
    onPay: (PaymentMethod, Long, Long) -> Unit,
    onSplit: () -> Unit,
    onReprint: () -> Unit,
    onDone: () -> Unit,
) {
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var amount by remember { mutableStateOf("") }
    var tendered by remember { mutableStateOf("") }

    LaunchedEffect(state.balanceMinor) { if (amount.isEmpty()) amount = state.balanceMinor.toString() }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AmountRow("Total", state.totalMinor)
        AmountRow("Paid", state.paidMinor)
        AmountRow("Balance", state.balanceMinor, emphasise = true)

        if (state.settled) {
            Text("Paid in full", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosOutlinedButton(text = "Reprint receipt", onClick = onReprint, modifier = Modifier.weight(1f))
                PosButton(text = "Done", onClick = onDone, modifier = Modifier.weight(1f))
            }
            return@Column
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentMethod.entries.forEach { m ->
                FilterChip(selected = m == method, onClick = { method = m }, label = { Text(m.name) })
            }
        }

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter(Char::isDigit) },
            label = { Text("Amount (minor units)") },
        )

        if (method == PaymentMethod.CASH) {
            OutlinedTextField(
                value = tendered,
                onValueChange = { tendered = it.filter(Char::isDigit) },
                label = { Text("Cash tendered") },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5_000L, 10_000L, 20_000L, 50_000L, 100_000L).forEach { denom ->
                    PosOutlinedButton(text = denom.toString(), onClick = { tendered = denom.toString() })
                }
            }
            val change = (tendered.toLongOrNull() ?: 0L) - (amount.toLongOrNull() ?: 0L)
            if (change > 0) AmountRow("Change", change, emphasise = true)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PosOutlinedButton(
                text = "Split bill",
                onClick = onSplit,
                enabled = state.lines.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
            PosButton(
                text = "Take payment",
                onClick = {
                    val a = amount.toLongOrNull() ?: 0L
                    val t = if (method == PaymentMethod.CASH) (tendered.toLongOrNull() ?: a) else a
                    onPay(method, a, t)
                    amount = ""
                    tendered = ""
                },
                enabled = (amount.toLongOrNull() ?: 0L) > 0,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AmountRow(label: String, amountMinor: Long, emphasise: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (emphasise) FontWeight.Bold else FontWeight.Normal)
        MoneyText(
            amountMinor.toString(),
            style = if (emphasise) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
        )
    }
}
