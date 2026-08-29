package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/**
 * A till session (FR-S1..S5). Opened with a declared float, closed with a blind counted
 * amount; `expectedCashMinor` / `varianceMinor` are computed at close. `closedAt IS NULL`
 * means the shift is still open. `businessDay` is the indexed reporting key.
 */
@Entity(
    tableName = "shift",
    foreignKeys = [
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["opened_by"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["closed_by"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("opened_by"),
        Index("closed_by"),
        Index("business_day"),
        Index("closed_at"),
    ],
)
data class ShiftEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("opened_by") val openedBy: String,
    @ColumnInfo("closed_by") val closedBy: String? = null,
    @ColumnInfo("opened_at") val openedAt: Long,
    @ColumnInfo("closed_at") val closedAt: Long? = null,
    @ColumnInfo("opening_float_minor") val openingFloatMinor: Long,
    @ColumnInfo("counted_cash_minor") val countedCashMinor: Long? = null,
    @ColumnInfo("expected_cash_minor") val expectedCashMinor: Long? = null,
    @ColumnInfo("variance_minor") val varianceMinor: Long? = null,
    @ColumnInfo("business_day") val businessDay: String,
    val note: String? = null,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
