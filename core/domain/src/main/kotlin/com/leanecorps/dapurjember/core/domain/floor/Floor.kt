package com.leanecorps.dapurjember.core.domain.floor

import kotlinx.coroutines.flow.Flow

enum class TableState { FREE, OCCUPIED, BILL_REQUESTED, NEEDS_CLEANING }

enum class TableType { DINE_IN, TAKEAWAY, DELIVERY }

data class FloorArea(
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
)

data class DiningTable(
    val id: String,
    val floorAreaId: String,
    val label: String,
    val seats: Int,
    /** Normalised 0..1 position so the plan scales across screen sizes. */
    val posX: Double,
    val posY: Double,
    val state: TableState,
    val type: TableType,
)

/** A floor area with its tables — the shape the floor screen renders (FR-T1). */
data class FloorAreaWithTables(
    val area: FloorArea,
    val tables: List<DiningTable>,
)

interface FloorRepository {

    fun observeFloor(): Flow<List<FloorAreaWithTables>>

    fun observeTablesForArea(areaId: String): Flow<List<DiningTable>>

    suspend fun getTable(tableId: String): DiningTable?

    suspend fun upsertArea(area: FloorArea)

    suspend fun upsertTable(table: DiningTable)

    suspend fun setTableState(tableId: String, state: TableState)

    suspend fun softDeleteArea(areaId: String)

    suspend fun softDeleteTable(tableId: String)
}
