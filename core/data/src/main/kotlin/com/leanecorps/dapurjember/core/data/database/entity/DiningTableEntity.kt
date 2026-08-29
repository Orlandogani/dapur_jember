package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/**
 * A table on the floor. `state` is FREE / OCCUPIED / BILL_REQUESTED / NEEDS_CLEANING;
 * `type` is DINE_IN / TAKEAWAY / DELIVERY (takeaway/delivery are pseudo-tables, FR-T3).
 * `posX`/`posY` are normalised 0..1 so the plan scales across screen sizes.
 */
@Entity(
    tableName = "dining_table",
    foreignKeys = [
        ForeignKey(
            entity = FloorAreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["floor_area_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("floor_area_id")],
)
data class DiningTableEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("floor_area_id") val floorAreaId: String,
    val label: String,
    val seats: Int,
    @ColumnInfo("pos_x") val posX: Double,
    @ColumnInfo("pos_y") val posY: Double,
    val state: String,
    val type: String,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
