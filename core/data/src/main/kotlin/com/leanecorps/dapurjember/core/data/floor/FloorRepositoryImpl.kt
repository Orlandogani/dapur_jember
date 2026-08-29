package com.leanecorps.dapurjember.core.data.floor

import androidx.room.withTransaction
import com.leanecorps.dapurjember.core.common.time.TimeProvider
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeOp
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.database.dao.DiningTableDao
import com.leanecorps.dapurjember.core.data.database.dao.FloorAreaDao
import com.leanecorps.dapurjember.core.data.database.entity.DiningTableEntity
import com.leanecorps.dapurjember.core.data.database.entity.FloorAreaEntity
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.floor.DiningTable
import com.leanecorps.dapurjember.core.domain.floor.FloorArea
import com.leanecorps.dapurjember.core.domain.floor.FloorAreaWithTables
import com.leanecorps.dapurjember.core.domain.floor.FloorRepository
import com.leanecorps.dapurjember.core.domain.floor.TableState
import com.leanecorps.dapurjember.core.domain.floor.TableType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal fun FloorAreaEntity.toDomain() = FloorArea(id = id, name = name, sortOrder = sortOrder)

internal fun DiningTableEntity.toDomain() = DiningTable(
    id = id,
    floorAreaId = floorAreaId,
    label = label,
    seats = seats,
    posX = posX,
    posY = posY,
    state = runCatching { TableState.valueOf(state) }.getOrDefault(TableState.FREE),
    type = runCatching { TableType.valueOf(type) }.getOrDefault(TableType.DINE_IN),
)

internal fun FloorArea.toEntity(existing: FloorAreaEntity?, now: Long, deviceId: String) = FloorAreaEntity(
    id = id,
    name = name,
    sortOrder = sortOrder,
    createdAt = existing?.createdAt ?: now,
    updatedAt = now,
    deletedAt = existing?.deletedAt,
    deviceId = existing?.deviceId ?: deviceId,
    revision = (existing?.revision ?: 0) + 1,
)

internal fun DiningTable.toEntity(existing: DiningTableEntity?, now: Long, deviceId: String) = DiningTableEntity(
    id = id,
    floorAreaId = floorAreaId,
    label = label,
    seats = seats,
    posX = posX,
    posY = posY,
    state = state.name,
    type = type.name,
    createdAt = existing?.createdAt ?: now,
    updatedAt = now,
    deletedAt = existing?.deletedAt,
    deviceId = existing?.deviceId ?: deviceId,
    revision = (existing?.revision ?: 0) + 1,
)

@Suppress("LongParameterList")
internal class FloorRepositoryImpl @Inject constructor(
    private val db: DapurJemberDatabase,
    private val areaDao: FloorAreaDao,
    private val tableDao: DiningTableDao,
    private val changeLog: ChangeLogRecorder,
    private val time: TimeProvider,
    private val deviceIds: DeviceIdProvider,
) : FloorRepository {

    override fun observeFloor(): Flow<List<FloorAreaWithTables>> =
        combine(areaDao.observeAll(), tableDao.observeAll()) { areas, tables ->
            val byArea = tables.groupBy { it.floorAreaId }
            areas.map { area ->
                FloorAreaWithTables(
                    area = area.toDomain(),
                    tables = byArea[area.id].orEmpty().map { it.toDomain() },
                )
            }
        }

    override fun observeTablesForArea(areaId: String): Flow<List<DiningTable>> =
        tableDao.observeByArea(areaId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getTable(tableId: String): DiningTable? = tableDao.getById(tableId)?.toDomain()

    override suspend fun upsertArea(area: FloorArea) = db.withTransaction {
        val existing = areaDao.getById(area.id)
        val now = time.nowMillis()
        areaDao.upsert(area.toEntity(existing, now, deviceIds.deviceId()))
        changeLog.record("floor_area", area.id, if (existing == null) ChangeOp.INSERT else ChangeOp.UPDATE, now)
    }

    override suspend fun upsertTable(table: DiningTable) = db.withTransaction {
        val existing = tableDao.getById(table.id)
        val now = time.nowMillis()
        tableDao.upsert(table.toEntity(existing, now, deviceIds.deviceId()))
        changeLog.record("dining_table", table.id, if (existing == null) ChangeOp.INSERT else ChangeOp.UPDATE, now)
    }

    override suspend fun setTableState(tableId: String, state: TableState) = db.withTransaction {
        val now = time.nowMillis()
        tableDao.updateState(tableId, state.name, now)
        changeLog.record("dining_table", tableId, ChangeOp.UPDATE, now)
    }

    override suspend fun softDeleteArea(areaId: String) = db.withTransaction {
        val now = time.nowMillis()
        areaDao.softDelete(areaId, now)
        changeLog.record("floor_area", areaId, ChangeOp.DELETE, now)
    }

    override suspend fun softDeleteTable(tableId: String) = db.withTransaction {
        val now = time.nowMillis()
        tableDao.softDelete(tableId, now)
        changeLog.record("dining_table", tableId, ChangeOp.DELETE, now)
    }
}
