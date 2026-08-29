package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only trail for privileged actions (`docs/3-data-model` §3.2, CLAUDE.md rule 10):
 * voids, discounts, price edits, stock adjustments, staff changes. No delete path exists in
 * the app. Like `change_log` it is **not** a [com.leanecorps.dapurjember.core.common.model.SyncableEntity].
 */
@Entity(
    tableName = "audit_log",
    foreignKeys = [
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["actor_staff_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("actor_staff_id"), Index("entity_type", "entity_id"), Index("created_at")],
)
data class AuditLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("actor_staff_id") val actorStaffId: String,
    val action: String,
    @ColumnInfo("entity_type") val entityType: String,
    @ColumnInfo("entity_id") val entityId: String,
    @ColumnInfo("before_json") val beforeJson: String? = null,
    @ColumnInfo("after_json") val afterJson: String? = null,
    val reason: String? = null,
    @ColumnInfo("created_at") val createdAt: Long,
)
