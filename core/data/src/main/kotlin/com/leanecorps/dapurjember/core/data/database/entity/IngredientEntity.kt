package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/**
 * A stock item (`docs/3-data-model` §3.7). Everything internal is in `baseUnit` (G / ML /
 * PIECE); the purchase unit + factor exist only for data entry ("1 sack = 25000 g").
 * `avgCostPerBaseMinor` is weighted-average cost (FR-I5). Negative stock is allowed (FR-I8).
 */
@Entity(
    tableName = "ingredient",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplier_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("supplier_id")],
)
data class IngredientEntity(
    @PrimaryKey override val id: String,
    val name: String,
    @ColumnInfo("base_unit") val baseUnit: String,
    @ColumnInfo("purchase_unit") val purchaseUnit: String,
    @ColumnInfo("purchase_to_base_factor") val purchaseToBaseFactor: Double,
    @ColumnInfo("current_stock_base") val currentStockBase: Double,
    @ColumnInfo("avg_cost_per_base_minor") val avgCostPerBaseMinor: Long,
    @ColumnInfo("low_stock_threshold_base") val lowStockThresholdBase: Double,
    @ColumnInfo("supplier_id") val supplierId: String? = null,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
