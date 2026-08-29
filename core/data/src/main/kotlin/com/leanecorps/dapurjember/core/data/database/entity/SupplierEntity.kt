package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/** A supplier — mostly v2 use, harmless to define now (`docs/3-data-model` §3.7). */
@Entity(tableName = "supplier")
data class SupplierEntity(
    @PrimaryKey override val id: String,
    val name: String,
    val phone: String? = null,
    val note: String? = null,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
