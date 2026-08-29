package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/**
 * A line on an order. Name/price are **snapshots** (FR-M5 — never join to the live menu).
 * `sent_at IS NULL` means the line has not been printed to the kitchen — this single column
 * implements FR-O3. `state` is ACTIVE / VOIDED.
 */
@Entity(
    tableName = "order_line",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MenuVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["menu_variant_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["added_by_staff_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("order_id"),
        Index("order_id", "sent_at"),
        Index("menu_variant_id"),
        Index("added_by_staff_id"),
    ],
)
data class OrderLineEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("order_id") val orderId: String,
    @ColumnInfo("menu_variant_id") val menuVariantId: String,
    @ColumnInfo("item_name_snapshot") val itemNameSnapshot: String,
    @ColumnInfo("variant_name_snapshot") val variantNameSnapshot: String,
    @ColumnInfo("unit_price_snapshot_minor") val unitPriceSnapshotMinor: Long,
    val qty: Int,
    @ColumnInfo("line_note") val lineNote: String? = null,
    val course: Int = 1,
    @ColumnInfo("sent_at") val sentAt: Long? = null,
    val state: String = "ACTIVE",
    @ColumnInfo("void_reason") val voidReason: String? = null,
    @ColumnInfo("added_by_staff_id") val addedByStaffId: String,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
