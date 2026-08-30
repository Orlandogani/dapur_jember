package com.leanecorps.dapurjember.feature.floor

import com.leanecorps.dapurjember.core.domain.floor.FloorAreaWithTables
import com.leanecorps.dapurjember.core.domain.floor.TableState

data class FloorUiState(
    val areas: List<FloorAreaUi> = emptyList(),
    /** Ingredients at or below their threshold (FR-I7). */
    val lowStockCount: Int = 0,
    val loading: Boolean = true,
) {
    val isEmpty: Boolean get() = !loading && areas.isEmpty()
}

data class FloorAreaUi(
    val id: String,
    val name: String,
    val tables: List<TableUi>,
)

data class TableUi(
    val id: String,
    val label: String,
    val seats: Int,
    val state: TableState,
)

internal fun FloorAreaWithTables.toUi() = FloorAreaUi(
    id = area.id,
    name = area.name,
    tables = tables.map { TableUi(id = it.id, label = it.label, seats = it.seats, state = it.state) },
)
