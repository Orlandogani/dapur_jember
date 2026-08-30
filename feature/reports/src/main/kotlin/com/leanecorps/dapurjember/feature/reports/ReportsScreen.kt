@file:OptIn(ExperimentalMaterial3Api::class)

package com.leanecorps.dapurjember.feature.reports

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.common.money.formatMoney
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.designsystem.component.SecureScreen
import com.leanecorps.dapurjember.core.domain.order.PaymentMethod
import com.leanecorps.dapurjember.core.domain.reports.AuditKind
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

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.reports_title)) }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosOutlinedButton(
                        text = stringResource(R.string.reports_action_previous_day),
                        onClick = { viewModel.shiftDay(-1) },
                    )
                    OutlinedTextField(
                        value = state.businessDay,
                        onValueChange = viewModel::setBusinessDay,
                        label = { Text(stringResource(R.string.reports_business_day_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    PosOutlinedButton(
                        text = stringResource(R.string.reports_action_next_day),
                        onClick = { viewModel.shiftDay(1) },
                    )
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
                        PosOutlinedButton(
                            text = stringResource(R.string.reports_action_back),
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                        )
                        PosOutlinedButton(
                            text = stringResource(R.string.reports_action_export_csv),
                            onClick = viewModel::exportCsv,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    Text(
                        stringResource(R.string.reports_sales_by_item),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (state.salesByItem.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.reports_sales_by_item_empty),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                items(state.salesByItem, key = { "item_${it.name}" }) { row ->
                    val gross = money(row.gross)
                    KeyValueRow(
                        label = stringResource(R.string.reports_qty_name, row.quantity, row.name),
                        value = row.marginPercent
                            ?.let { stringResource(R.string.reports_margin_suffix, gross, formatPercent(it)) }
                            ?: gross,
                    )
                }

                if (state.salesByCategory.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.reports_sales_by_category),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    items(state.salesByCategory, key = { "cat_${it.name}" }) { row ->
                        KeyValueRow(stringResource(R.string.reports_qty_name, row.quantity, row.name), money(row.gross))
                    }
                }

                item { Text(stringResource(R.string.reports_audit), style = MaterialTheme.typography.titleMedium) }
                if (state.audit.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.reports_audit_empty),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                items(state.audit, key = { "audit_${it.kind}_${it.at}_${it.description}" }) { entry ->
                    val kind = stringResource(entry.kind.labelRes())
                    val reason = entry.reason
                    KeyValueRow(
                        label = if (reason == null) {
                            stringResource(R.string.reports_audit_row, kind, entry.description, entry.staffName)
                        } else {
                            stringResource(
                                R.string.reports_audit_row_reason,
                                kind,
                                entry.description,
                                entry.staffName,
                                reason,
                            )
                        },
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
            KeyValueRow(stringResource(R.string.reports_gross_revenue), money(summary.grossRevenue), emphasise = true)
            KeyValueRow(stringResource(R.string.reports_paid_orders), summary.orderCount.toString())
            KeyValueRow(stringResource(R.string.reports_covers), summary.covers.toString())
            KeyValueRow(stringResource(R.string.reports_average_ticket), money(summary.averageTicket))
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            KeyValueRow(stringResource(R.string.reports_cogs), money(summary.cogs))
            KeyValueRow(stringResource(R.string.reports_gross_profit), money(summary.grossProfit), emphasise = true)
            KeyValueRow(
                stringResource(R.string.reports_gross_margin),
                summary.grossMarginPercent?.let(::formatPercent) ?: stringResource(R.string.reports_value_none),
            )
            if (summary.cogs.isZero && summary.orderCount > 0) {
                Text(
                    stringResource(R.string.reports_no_recipe_costs),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text(stringResource(R.string.reports_payment_mix), style = MaterialTheme.typography.labelLarge)
            if (summary.paymentMix.isEmpty()) {
                Text(stringResource(R.string.reports_no_payments), style = MaterialTheme.typography.bodySmall)
            }
            summary.paymentMix.forEach { row ->
                KeyValueRow(stringResource(row.method.labelRes()), money(row.amount))
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            KeyValueRow(
                stringResource(R.string.reports_discounts),
                stringResource(R.string.reports_discounts_value, summary.discountCount, money(summary.discountTotal)),
            )
            KeyValueRow(
                stringResource(R.string.reports_voided),
                stringResource(R.string.reports_voided_value, summary.voidedOrders, summary.voidedLines),
            )
        }
    }
}

@Composable
private fun PermissionDenied(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.reports_title)) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.reports_no_permission),
                style = MaterialTheme.typography.bodyLarge,
            )
            PosOutlinedButton(text = stringResource(R.string.reports_action_back), onClick = onBack)
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

/** Display labels. The domain stores stable enum names; translation belongs to the UI (NFR8). */
@StringRes
private fun AuditKind.labelRes(): Int = when (this) {
    AuditKind.VOID -> R.string.audit_kind_void
    AuditKind.DISCOUNT -> R.string.audit_kind_discount
}

@StringRes
private fun PaymentMethod.labelRes(): Int = when (this) {
    PaymentMethod.CASH -> R.string.payment_method_cash
    PaymentMethod.CARD -> R.string.payment_method_card
    PaymentMethod.EWALLET -> R.string.payment_method_ewallet
    PaymentMethod.OTHER -> R.string.payment_method_other
}
