package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/**
 * An order — `docs/3-data-model` §3.5, "the heart of the schema". `state` holds the
 * `OrderStateMachine` value. The `*_minor` totals are **denormalised snapshots** recomputed
 * on every mutation so a historical receipt reproduces byte-for-byte after tax rules change.
 * `business_day` is the indexed reporting key (never query on a `paid_at` range).
 */
@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = DiningTableEntity::class,
            parentColumns = ["id"],
            childColumns = ["dining_table_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shift_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["opened_by_staff_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["closed_by_staff_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("business_day"),
        Index("state"),
        Index("dining_table_id", "state"),
        Index("shift_id"),
        Index("opened_by_staff_id"),
        Index("closed_by_staff_id"),
    ],
)
data class OrderEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("order_number") val orderNumber: String,
    @ColumnInfo("dining_table_id") val diningTableId: String? = null,
    @ColumnInfo("shift_id") val shiftId: String,
    @ColumnInfo("opened_by_staff_id") val openedByStaffId: String,
    @ColumnInfo("closed_by_staff_id") val closedByStaffId: String? = null,
    val state: String,
    @ColumnInfo("guest_count") val guestCount: Int,
    @ColumnInfo("business_day") val businessDay: String,
    @ColumnInfo("opened_at") val openedAt: Long? = null,
    @ColumnInfo("sent_at") val sentAt: Long? = null,
    @ColumnInfo("paid_at") val paidAt: Long? = null,
    @ColumnInfo("closed_at") val closedAt: Long? = null,
    @ColumnInfo("subtotal_minor") val subtotalMinor: Long = 0,
    @ColumnInfo("discount_minor") val discountMinor: Long = 0,
    @ColumnInfo("service_charge_minor") val serviceChargeMinor: Long = 0,
    @ColumnInfo("tax_minor") val taxMinor: Long = 0,
    @ColumnInfo("rounding_minor") val roundingMinor: Long = 0,
    @ColumnInfo("total_minor") val totalMinor: Long = 0,
    @ColumnInfo("void_reason") val voidReason: String? = null,
    val note: String? = null,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
