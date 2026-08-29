@file:OptIn(ExperimentalMaterial3Api::class)

package com.leanecorps.dapurjember.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.common.money.formatMoney
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.domain.reports.DailySummary

@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val money = { m: Money -> formatMoney(m, state.currencyCode, state.currencyMinorUnits) }

    Scaffold(topBar = { TopAppBar(title = { Text("Daily summary") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosOutlinedButton(text = "‹", onClick = { viewModel.shiftDay(-1) })
                    OutlinedTextField(
                        value = state.businessDay,
                        onValueChange = viewModel::setBusinessDay,
                        label = { Text("Business day (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    PosOutlinedButton(text = "›", onClick = { viewModel.shiftDay(1) })
                }
            }

            if (state.loading) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            state.summary?.let { summary ->
                item { SummaryCard(summary, money) }
                item { PosOutlinedButton(text = "Back", onClick = onBack) }
                item { Text("Sales by item", style = MaterialTheme.typography.titleMedium) }
                if (state.salesByItem.isEmpty()) {
                    item { Text("Nothing sold on this day.", style = MaterialTheme.typography.bodyMedium) }
                }
                items(state.salesByItem, key = { it.name }) { row ->
                    KeyValueRow("${row.quantity}× ${row.name}", money(row.gross))
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: DailySummary, money: (Money) -> String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyValueRow("Gross revenue", money(summary.grossRevenue), emphasise = true)
            KeyValueRow("Paid orders", summary.orderCount.toString())
            KeyValueRow("Covers", summary.covers.toString())
            KeyValueRow("Average ticket", money(summary.averageTicket))
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text("Payment mix", style = MaterialTheme.typography.labelLarge)
            if (summary.paymentMix.isEmpty()) {
                Text("No payments recorded.", style = MaterialTheme.typography.bodySmall)
            }
            summary.paymentMix.forEach { row -> KeyValueRow(row.method.name, money(row.amount)) }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            KeyValueRow("Discounts", "${summary.discountCount} · ${money(summary.discountTotal)}")
            KeyValueRow("Voided orders / lines", "${summary.voidedOrders} / ${summary.voidedLines}")
        }
    }
}

@Composable
private fun KeyValueRow(label: String, value: String, emphasise: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (emphasise) FontWeight.Bold else FontWeight.Normal)
        Text(
            value,
            fontWeight = if (emphasise) FontWeight.Bold else FontWeight.Normal,
            style = if (emphasise) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
    }
}
