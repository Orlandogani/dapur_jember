package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/** A modifier chosen on an order line; name and price delta are snapshots. */
@Entity(
    tableName = "order_line_modifier",
    foreignKeys = [
        ForeignKey(
            entity = OrderLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_line_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ModifierEntity::class,
            parentColumns = ["id"],
            childColumns = ["modifier_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("order_line_id"), Index("modifier_id")],
)
data class OrderLineModifierEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("order_line_id") val orderLineId: String,
    @ColumnInfo("modifier_id") val modifierId: String,
    @ColumnInfo("name_snapshot") val nameSnapshot: String,
    @ColumnInfo("price_delta_snapshot_minor") val priceDeltaSnapshotMinor: Long,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
