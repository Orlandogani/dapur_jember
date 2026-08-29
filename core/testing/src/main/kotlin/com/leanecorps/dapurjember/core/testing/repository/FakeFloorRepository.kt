package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.domain.floor.DiningTable
import com.leanecorps.dapurjember.core.domain.floor.FloorArea
import com.leanecorps.dapurjember.core.domain.floor.FloorAreaWithTables
import com.leanecorps.dapurjember.core.domain.floor.FloorRepository
import com.leanecorps.dapurjember.core.domain.floor.TableState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [FloorRepository] for ViewModel tests. */
class FakeFloorRepository : FloorRepository {

    private val areas = MutableStateFlow<List<FloorArea>>(emptyList())
    private val tables = MutableStateFlow<List<DiningTable>>(emptyList())

    override fun observeFloor(): Flow<List<FloorAreaWithTables>> =
        combine(areas, tables) { areaList, tableList ->
            val byArea = tableList.groupBy { it.floorAreaId }
            areaList.map { FloorAreaWithTables(it, byArea[it.id].orEmpty()) }
        }

    override fun observeTablesForArea(areaId: String): Flow<List<DiningTable>> =
        tables.map { list -> list.filter { it.floorAreaId == areaId } }

    override suspend fun getTable(tableId: String): DiningTable? = tables.value.firstOrNull { it.id == tableId }

    override suspend fun upsertArea(area: FloorArea) =
        areas.update { it.filterNot { a -> a.id == area.id } + area }

    override suspend fun upsertTable(table: DiningTable) =
        tables.update { it.filterNot { t -> t.id == table.id } + table }

    override suspend fun setTableState(tableId: String, state: TableState) =
        tables.update { list -> list.map { if (it.id == tableId) it.copy(state = state) else it } }

    override suspend fun softDeleteArea(areaId: String) = areas.update { it.filterNot { a -> a.id == areaId } }

    override suspend fun softDeleteTable(tableId: String) = tables.update { it.filterNot { t -> t.id == tableId } }
}
