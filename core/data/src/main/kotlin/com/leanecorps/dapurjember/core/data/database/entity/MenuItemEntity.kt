package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

@Entity(
    tableName = "menu_item",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("category_id")],
)
data class MenuItemEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("category_id") val categoryId: String,
    val name: String,
    val description: String?,
    @ColumnInfo("image_path") val imagePath: String?,
    @ColumnInfo("sort_order") val sortOrder: Int,
    val available: Boolean,
    @ColumnInfo("tax_exempt") val taxExempt: Boolean,
    @ColumnInfo("track_stock") val trackStock: Boolean,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
