package com.leanecorps.dapurjember.core.data.database

import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.data.database.dao.ChangeLogDao
import com.leanecorps.dapurjember.core.data.database.entity.ChangeLogEntity
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import javax.inject.Inject

/** The op recorded in `change_log` for a mutation. */
enum class ChangeOp { INSERT, UPDATE, DELETE }

/**
 * Appends a `change_log` row for a mutation. Repositories call this **inside the same
 * transaction** as the write it describes (CLAUDE.md rule 5) — nothing reads the log in v1;
 * the v2 sync engine drains it.
 */
internal class ChangeLogRecorder @Inject constructor(
    private val changeLogDao: ChangeLogDao,
    private val deviceIdProvider: DeviceIdProvider,
) {
    suspend fun record(entityType: String, entityId: String, op: ChangeOp, at: Long) {
        changeLogDao.insert(
            ChangeLogEntity(
                id = UuidV7.generate(),
                entityType = entityType,
                entityId = entityId,
                op = op.name,
                timestamp = at,
                deviceId = deviceIdProvider.deviceId(),
            ),
        )
    }
}
