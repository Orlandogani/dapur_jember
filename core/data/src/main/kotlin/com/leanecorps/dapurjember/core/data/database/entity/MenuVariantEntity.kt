package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

@Entity(
    tableName = "menu_variant",
    foreignKeys = [
        ForeignKey(
            entity = MenuItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["menu_item_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("menu_item_id")],
)
data class MenuVariantEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("menu_item_id") val menuItemId: String,
    val name: String,
    @ColumnInfo("price_minor") val priceMinor: Long,
    val sku: String?,
    @ColumnInfo("sort_order") val sortOrder: Int,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
