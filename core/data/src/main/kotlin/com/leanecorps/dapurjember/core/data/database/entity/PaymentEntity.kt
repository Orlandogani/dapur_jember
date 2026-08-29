package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/**
 * One payment against an order (FR-P1..P3). `method` is CASH / CARD / EWALLET / OTHER; v1
 * only records it. Multiple rows per order settle a split or partial payment.
 */
@Entity(
    tableName = "payment",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["staff_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("order_id"), Index("staff_id")],
)
data class PaymentEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("order_id") val orderId: String,
    val method: String,
    @ColumnInfo("amount_minor") val amountMinor: Long,
    @ColumnInfo("tendered_minor") val tenderedMinor: Long,
    @ColumnInfo("change_minor") val changeMinor: Long,
    val reference: String? = null,
    @ColumnInfo("staff_id") val staffId: String,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
