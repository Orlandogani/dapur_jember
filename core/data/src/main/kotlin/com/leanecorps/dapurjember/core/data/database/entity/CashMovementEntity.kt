package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/** Cash in/out of the drawer during a shift — bank drop, petty cash (FR-S2). `direction` is IN / OUT. */
@Entity(
    tableName = "cash_movement",
    foreignKeys = [
        ForeignKey(
            entity = ShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shift_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["staff_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("shift_id"), Index("staff_id")],
)
data class CashMovementEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("shift_id") val shiftId: String,
    val direction: String,
    @ColumnInfo("amount_minor") val amountMinor: Long,
    val reason: String,
    @ColumnInfo("staff_id") val staffId: String,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
