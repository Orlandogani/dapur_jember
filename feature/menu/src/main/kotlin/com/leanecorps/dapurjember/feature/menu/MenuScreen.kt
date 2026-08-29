package com.leanecorps.dapurjember.feature.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MenuScreen(viewModel: MenuViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MenuScreen(state = state, onToggleAvailability = viewModel::setAvailability)
}

@Composable
internal fun MenuScreen(
    state: MenuUiState,
    onToggleAvailability: (itemId: String, available: Boolean) -> Unit,
) {
    when {
        state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

        state.isEmpty -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("No menu items yet", style = MaterialTheme.typography.bodyLarge)
        }

        else -> LazyColumn(Modifier.fillMaxSize()) {
            state.sections.forEach { section ->
                item(key = "h_${section.id}") {
                    Text(
                        text = section.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(section.items, key = { it.id }) { item ->
                    MenuItemRow(item = item, onToggle = { onToggleAvailability(item.id, it) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MenuItemRow(item: MenuItemUi, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (item.available) TextDecoration.None else TextDecoration.LineThrough,
        )
        Switch(checked = item.available, onCheckedChange = onToggle)
    }
}
