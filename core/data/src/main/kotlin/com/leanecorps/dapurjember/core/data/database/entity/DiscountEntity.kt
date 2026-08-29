package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/**
 * A discount applied to a line (`order_line_id` set) or the whole bill (`order_line_id` null),
 * FR-P4. `value` is basis points when `type` is PERCENT, minor units when FIXED;
 * `computed_minor` is the resolved amount at the time it was applied.
 */
@Entity(
    tableName = "discount",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = OrderLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_line_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorised_by_staff_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("order_id"), Index("order_line_id"), Index("authorised_by_staff_id")],
)
data class DiscountEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("order_id") val orderId: String,
    @ColumnInfo("order_line_id") val orderLineId: String? = null,
    val type: String,
    val value: Long,
    @ColumnInfo("computed_minor") val computedMinor: Long,
    val reason: String,
    @ColumnInfo("authorised_by_staff_id") val authorisedByStaffId: String,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
