package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/**
 * The audit trail for stock (`docs/3-data-model` §3.7). Every change to
 * `ingredient.current_stock_base` writes one of these — that is what makes "why is my
 * chicken at 4kg" answerable (FR-I6). `reason` is
 * SALE / PURCHASE / WASTE / SPOILAGE / STAFF_MEAL / COUNT_CORRECTION / OPENING.
 */
@Entity(
    tableName = "stock_movement",
    foreignKeys = [
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredient_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = OrderLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_line_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["staff_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("ingredient_id", "created_at"),
        Index("order_line_id"),
        Index("staff_id"),
    ],
)
data class StockMovementEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("ingredient_id") val ingredientId: String,
    @ColumnInfo("qty_base_delta") val qtyBaseDelta: Double,
    val reason: String,
    @ColumnInfo("order_line_id") val orderLineId: String? = null,
    @ColumnInfo("unit_cost_minor") val unitCostMinor: Long,
    @ColumnInfo("staff_id") val staffId: String,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
