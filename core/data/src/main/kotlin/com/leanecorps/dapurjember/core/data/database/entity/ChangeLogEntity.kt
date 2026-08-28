package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only outbox (`docs/3-data-model` 3.8). Every mutation writes one of these in the
 * same transaction (CLAUDE.md rule 5). Nothing reads it in v1; the v2 sync engine drains it.
 *
 * Deliberately **not** a [com.leanecorps.dapurjember.core.common.model.SyncableEntity] — the
 * sync log is not itself synced, soft-deleted, or revised.
 */
@Entity(
    tableName = "change_log",
    indices = [
        Index("synced_at"),
        Index("entity_type", "entity_id"),
    ],
)
data class ChangeLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("entity_type") val entityType: String,
    @ColumnInfo("entity_id") val entityId: String,
    val op: String,
    val timestamp: Long,
    @ColumnInfo("device_id") val deviceId: String,
    @ColumnInfo("synced_at") val syncedAt: Long? = null,
)
