package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

@Entity(
    tableName = "modifier",
    foreignKeys = [
        ForeignKey(
            entity = ModifierGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["modifier_group_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("modifier_group_id")],
)
data class ModifierEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo("modifier_group_id") val modifierGroupId: String,
    val name: String,
    @ColumnInfo("price_delta_minor") val priceDeltaMinor: Long,
    @ColumnInfo("sort_order") val sortOrder: Int,
    @ColumnInfo("default_selected") val defaultSelected: Boolean,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
