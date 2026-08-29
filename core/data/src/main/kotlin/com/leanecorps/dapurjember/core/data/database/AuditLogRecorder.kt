package com.leanecorps.dapurjember.core.data.database

import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.data.database.dao.AuditLogDao
import com.leanecorps.dapurjember.core.data.database.entity.AuditLogEntity
import javax.inject.Inject

/**
 * Appends an `audit_log` row for a privileged action (void, discount, price edit, stock
 * adjustment, staff change — CLAUDE.md rule 10). Called inside the same transaction as the
 * action. Append-only; the app exposes no delete path.
 */
internal class AuditLogRecorder @Inject constructor(
    private val auditLogDao: AuditLogDao,
) {
    @Suppress("LongParameterList")
    suspend fun record(
        actorStaffId: String,
        action: String,
        entityType: String,
        entityId: String,
        at: Long,
        reason: String? = null,
        beforeJson: String? = null,
        afterJson: String? = null,
    ) {
        auditLogDao.insert(
            AuditLogEntity(
                id = UuidV7.generate(),
                actorStaffId = actorStaffId,
                action = action,
                entityType = entityType,
                entityId = entityId,
                beforeJson = beforeJson,
                afterJson = afterJson,
                reason = reason,
                createdAt = at,
            ),
        )
    }
}
