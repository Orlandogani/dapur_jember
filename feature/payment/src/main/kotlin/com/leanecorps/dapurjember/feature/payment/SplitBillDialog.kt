@file:OptIn(ExperimentalLayoutApi::class)

package com.leanecorps.dapurjember.feature.payment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leanecorps.dapurjember.core.designsystem.component.MoneyText
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.domain.order.PaymentMethod

/**
 * Split-bill sheet (S09). Parts are proposed, never persisted — each is taken as an ordinary
 * partial payment, so a bill can be split three ways and paid three different ways.
 */
@Composable
@Suppress("LongParameterList")
internal fun SplitBillDialog(
    state: PaymentUiState,
    split: SplitUiState,
    onMode: (SplitMode) -> Unit,
    onWays: (Int) -> Unit,
    onCycleLine: (String) -> Unit,
    onPayPart: (Int, PaymentMethod) -> Unit,
    onDismiss: () -> Unit,
) {
    var method by remember { mutableStateOf(PaymentMethod.CASH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { PosButton(text = stringResource(R.string.payment_action_done), onClick = onDismiss) },
        title = { Text(stringResource(R.string.split_title)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = split.mode == SplitMode.EVENLY,
                            onClick = { onMode(SplitMode.EVENLY) },
                            label = { Text(stringResource(R.string.split_mode_evenly)) },
                        )
                        FilterChip(
                            selected = split.mode == SplitMode.BY_ITEM,
                            onClick = { onMode(SplitMode.BY_ITEM) },
                            label = { Text(stringResource(R.string.split_mode_by_item)) },
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.split_ways), Modifier.padding(end = 8.dp))
                        PosOutlinedButton(
                            text = stringResource(R.string.split_decrease),
                            onClick = { onWays(split.ways - 1) },
                        )
                        Text(
                            split.ways.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        PosOutlinedButton(
                            text = stringResource(R.string.split_increase),
                            onClick = { onWays(split.ways + 1) },
                        )
                    }
                }

                if (split.mode == SplitMode.BY_ITEM) {
                    item {
                        Text(
                            stringResource(R.string.split_assign_hint),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    itemsIndexed(state.lines, key = { _, line -> line.id }) { _, line ->
                        val guest = split.assignment[line.id]
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onCycleLine(line.id) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(line.name, Modifier.weight(1f))
                            Text(
                                guest?.let { stringResource(R.string.split_guest, it + 1) }
                                    ?: stringResource(R.string.split_shared),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            MoneyText(line.lineTotalMinor.toString(), Modifier.padding(start = 8.dp))
                        }
                    }
                }

                item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
                item {
                    // The whole point of the feature: the parts must add up to the bill.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.split_parts_total), fontWeight = FontWeight.Bold)
                        MoneyText(split.partsTotalMinor.toString())
                    }
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentMethod.entries.forEach { m ->
                            FilterChip(
                                selected = m == method,
                                onClick = { method = m },
                                label = { Text(m.name) },
                            )
                        }
                    }
                }

                itemsIndexed(split.parts, key = { index, _ -> index }) { index, part ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.split_guest, part.guestIndex + 1), Modifier.weight(1f))
                        MoneyText(part.amountMinor.toString(), Modifier.padding(end = 8.dp))
                        if (part.paid) {
                            Text(stringResource(R.string.split_part_paid), style = MaterialTheme.typography.labelMedium)
                        } else {
                            PosOutlinedButton(
                                text = stringResource(R.string.split_action_take_part),
                                onClick = { onPayPart(index, method) },
                            )
                        }
                    }
                }
            }
        },
    )
}
