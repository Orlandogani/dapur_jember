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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.common.money.formatMoney
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.designsystem.component.SecureScreen
import com.leanecorps.dapurjember.core.domain.reports.DailySummary
import java.util.Locale

@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val canView by viewModel.canView.collectAsStateWithLifecycle()
    SecureScreen()
    val money = { m: Money -> formatMoney(m, state.currencyCode, state.currencyMinorUnits) }
    val context = LocalContext.current

    if (!canView) {
        PermissionDenied(onBack = onBack)
        return
    }

    LaunchedEffect(Unit) {
        viewModel.csvExports.collect { export -> shareCsv(context, export) }
    }

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
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PosOutlinedButton(text = "Back", onClick = onBack, modifier = Modifier.weight(1f))
                        PosOutlinedButton(
                            text = "Export CSV",
                            onClick = viewModel::exportCsv,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item { Text("Sales by item", style = MaterialTheme.typography.titleMedium) }
                if (state.salesByItem.isEmpty()) {
                    item { Text("Nothing sold on this day.", style = MaterialTheme.typography.bodyMedium) }
                }
                items(state.salesByItem, key = { "item_${it.name}" }) { row ->
                    val margin = row.marginPercent?.let { "  ·  ${formatPercent(it)} margin" }.orEmpty()
                    KeyValueRow(label = "${row.quantity}× ${row.name}", value = money(row.gross) + margin)
                }

                if (state.salesByCategory.isNotEmpty()) {
                    item { Text("Sales by category", style = MaterialTheme.typography.titleMedium) }
                    items(state.salesByCategory, key = { "cat_${it.name}" }) { row ->
                        KeyValueRow("${row.quantity}× ${row.name}", money(row.gross))
                    }
                }

                item { Text("Voids & discounts", style = MaterialTheme.typography.titleMedium) }
                if (state.audit.isEmpty()) {
                    item { Text("No voids or discounts today.", style = MaterialTheme.typography.bodyMedium) }
                }
                items(state.audit, key = { "audit_${it.kind}_${it.at}_${it.description}" }) { entry ->
                    KeyValueRow(
                        label = "${entry.kind.name} · ${entry.description} · ${entry.staffName}" +
                            (entry.reason?.let { " ($it)" } ?: ""),
                        value = money(entry.amount),
                    )
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
            KeyValueRow("COGS", money(summary.cogs))
            KeyValueRow("Gross profit", money(summary.grossProfit), emphasise = true)
            KeyValueRow("Gross margin", summary.grossMarginPercent?.let(::formatPercent) ?: "—")
            if (summary.cogs.isZero && summary.orderCount > 0) {
                Text(
                    "No recipe costs recorded — add recipes to menu items to see food cost.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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
private fun PermissionDenied(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Daily summary") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Reports are available to managers and owners.",
                style = MaterialTheme.typography.bodyLarge,
            )
            PosOutlinedButton(text = "Back", onClick = onBack)
        }
    }
}

/** One decimal place, locale-independent so the number reads the same on every device. */
private fun formatPercent(value: Double): String = String.format(Locale.ROOT, "%.1f%%", value)

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
