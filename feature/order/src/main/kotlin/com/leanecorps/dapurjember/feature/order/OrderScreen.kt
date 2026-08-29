package com.leanecorps.dapurjember.feature.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.MoneyText
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.designsystem.theme.PosTouchTarget

@Composable
fun OrderScreen(
    onCheckout: (orderId: String) -> Unit,
    viewModel: OrderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrderScreen(
        state = state,
        onSelectCategory = viewModel::selectCategory,
        onAddTile = viewModel::addTile,
        onIncrement = viewModel::increment,
        onDecrement = viewModel::decrement,
        onSend = viewModel::send,
        onCheckout = onCheckout,
    )
}

@Composable
@Suppress("LongParameterList")
internal fun OrderScreen(
    state: OrderUiState,
    onSelectCategory: (String) -> Unit,
    onAddTile: (BoardTileUi) -> Unit,
    onIncrement: (OrderLineUi) -> Unit,
    onDecrement: (OrderLineUi) -> Unit,
    onSend: () -> Unit,
    onCheckout: (String) -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        CategoryRail(
            categories = state.categories,
            selectedId = state.selectedCategoryId,
            onSelect = onSelectCategory,
            modifier = Modifier.width(160.dp).fillMaxHeight(),
        )
        HorizontalDivider(Modifier.fillMaxHeight().width(1.dp))
        ItemGrid(
            tiles = state.board,
            onTap = onAddTile,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        HorizontalDivider(Modifier.fillMaxHeight().width(1.dp))
        OrderRail(
            state = state,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
            onSend = onSend,
            onCheckout = onCheckout,
            modifier = Modifier.width(300.dp).fillMaxHeight(),
        )
    }
}

@Composable
private fun CategoryRail(
    categories: List<CategoryTabUi>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier) {
        items(categories, key = { it.id }) { category ->
            val selected = category.id == selectedId
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(category.id) }
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    )
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun ItemGrid(
    tiles: List<BoardTileUi>,
    onTap: (BoardTileUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(tiles, key = { it.itemId }) { tile ->
            Card(
                onClick = { if (tile.available) onTap(tile) },
                enabled = tile.available,
                modifier = Modifier.fillMaxWidth().padding(0.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = tile.name,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (tile.available) TextDecoration.None else TextDecoration.LineThrough,
                    )
                    tile.priceMinor?.let { MoneyText(it.toString(), style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun OrderRail(
    state: OrderUiState,
    onIncrement: (OrderLineUi) -> Unit,
    onDecrement: (OrderLineUi) -> Unit,
    onSend: () -> Unit,
    onCheckout: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(12.dp)) {
        Text("Order ${state.orderNumber}", style = MaterialTheme.typography.titleLarge)
        Text("${state.guestCount} guests · ${state.state}", style = MaterialTheme.typography.labelMedium)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(state.lines, key = { it.id }) { line -> OrderLineRow(line, onIncrement, onDecrement) }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        TotalRow("Subtotal", state.totals.subtotalMinor)
        if (state.totals.discountMinor != 0L) TotalRow("Discount", -state.totals.discountMinor)
        if (state.totals.serviceChargeMinor != 0L) TotalRow("Service", state.totals.serviceChargeMinor)
        if (state.totals.taxMinor != 0L) TotalRow("Tax", state.totals.taxMinor)
        TotalRow("Total", state.totals.totalMinor, emphasise = true)

        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PosOutlinedButton(
                text = "Send",
                onClick = onSend,
                enabled = state.canSend,
                modifier = Modifier.weight(1f),
            )
            PosButton(
                text = "Pay",
                onClick = { onCheckout(state.orderId) },
                enabled = state.canPay,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OrderLineRow(
    line: OrderLineUi,
    onIncrement: (OrderLineUi) -> Unit,
    onDecrement: (OrderLineUi) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "${line.quantity}× ${line.name}",
                textDecoration = if (line.voided) TextDecoration.LineThrough else TextDecoration.None,
            )
            line.note?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
        }
        if (!line.voided && !line.sent) {
            PosOutlinedButton("–", { onDecrement(line) }, Modifier.width(PosTouchTarget))
            PosOutlinedButton("+", { onIncrement(line) }, Modifier.width(PosTouchTarget))
        }
        MoneyText(line.lineTotalMinor.toString(), Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun TotalRow(label: String, amountMinor: Long, emphasise: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (emphasise) FontWeight.Bold else FontWeight.Normal)
        MoneyText(
            text = amountMinor.toString(),
            style = if (emphasise) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
    }
}
