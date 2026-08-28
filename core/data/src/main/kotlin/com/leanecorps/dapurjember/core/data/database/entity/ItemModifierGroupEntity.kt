package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/** Join row attaching a reusable [ModifierGroupEntity] to a [MenuItemEntity]. */
@Entity(
    tableName = "item_modifier_group",
    foreignKeys = [
        ForeignKey(
            entity = MenuItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["menu_item_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ModifierGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["modifier_group_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("menu_item_id"),
        Index("modifier_group_id"),
        Index(value = ["menu_item_id", "modifier_group_id"], unique = true),
    ],
)
data class ItemModifierGroupEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("menu_item_id") val menuItemId: String,
    @ColumnInfo("modifier_group_id") val modifierGroupId: String,
    @ColumnInfo("sort_order") val sortOrder: Int,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
