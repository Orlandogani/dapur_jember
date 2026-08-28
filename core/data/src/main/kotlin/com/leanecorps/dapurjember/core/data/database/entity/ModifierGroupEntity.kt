package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

@Entity(tableName = "modifier_group")
data class ModifierGroupEntity(
    @PrimaryKey override val id: String,
    val name: String,
    @ColumnInfo("min_select") val minSelect: Int,
    @ColumnInfo("max_select") val maxSelect: Int,
    val required: Boolean,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
