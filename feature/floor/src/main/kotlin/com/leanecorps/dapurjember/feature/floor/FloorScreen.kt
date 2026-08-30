package com.leanecorps.dapurjember.feature.floor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.StatusChip
import com.leanecorps.dapurjember.core.designsystem.theme.LocalPosStatusColors
import com.leanecorps.dapurjember.core.designsystem.theme.PosTouchTarget
import com.leanecorps.dapurjember.core.domain.floor.TableState

@Composable
@Suppress("LongParameterList")
fun FloorScreen(
    backupOverdue: Boolean,
    onDismissBackupReminder: () -> Unit,
    onOpenTable: (tableId: String) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: FloorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FloorScreen(
        state = state,
        backupOverdue = backupOverdue,
        onDismissBackupReminder = onDismissBackupReminder,
        onOpenTable = onOpenTable,
        onOpenMenu = onOpenMenu,
        onOpenReports = onOpenReports,
        onOpenInventory = onOpenInventory,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
internal fun FloorScreen(
    state: FloorUiState,
    backupOverdue: Boolean,
    onDismissBackupReminder: () -> Unit,
    onOpenTable: (tableId: String) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Floor") },
                actions = {
                    TextButton(onClick = onOpenMenu) { Text("Menu") }
                    TextButton(onClick = onOpenReports) { Text("Reports") }
                    TextButton(onClick = onOpenInventory) {
                        Text(
                            if (state.lowStockCount > 0) "Inventory (${state.lowStockCount})" else "Inventory",
                            color = if (state.lowStockCount > 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                    TextButton(onClick = onOpenSettings) { Text("Settings") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // FR-D4: nag after a week without a backup. Non-blocking — never trap a busy floor.
            if (backupOverdue) {
                BackupReminderBanner(onOpenSettings = onOpenSettings, onDismiss = onDismissBackupReminder)
            }
            Box(Modifier.fillMaxSize()) {
                when {
                    state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

                    state.isEmpty -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("No tables configured yet", style = MaterialTheme.typography.bodyLarge)
                    }

                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 132.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.areas.forEach { area ->
                            item(span = { GridItemSpan(maxLineSpan) }, key = "header_${area.id}") {
                                Text(
                                    text = area.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            items(area.tables, key = { it.id }) { table ->
                                TableCard(table = table, onClick = { onOpenTable(table.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupReminderBanner(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "It has been a while since your last backup.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenSettings) { Text("Back up") }
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    }
}

@Composable
private fun TableCard(table: TableUi, onClick: () -> Unit) {
    val status = LocalPosStatusColors.current
    val color = when (table.state) {
        TableState.FREE -> status.free
        TableState.OCCUPIED -> status.occupied
        TableState.BILL_REQUESTED -> status.attention
        TableState.NEEDS_CLEANING -> status.needsCleaning
    }
    Card(
        onClick = onClick,
        modifier = Modifier.heightIn(min = PosTouchTarget * 2),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(table.label, style = MaterialTheme.typography.titleLarge)
            Text("${table.seats} seats", style = MaterialTheme.typography.labelMedium)
            StatusChip(
                label = table.state.name.replace('_', ' '),
                color = color,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
