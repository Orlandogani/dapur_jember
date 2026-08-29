package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/**
 * A staff member. `pinHash` is Argon2id (hashing lives in :feature:auth). `role` is
 * OWNER / MANAGER / CASHIER / WAITER; `permissionsJson` holds per-user overrides on top of
 * the role default.
 */
@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey override val id: String,
    val name: String,
    @ColumnInfo("pin_hash") val pinHash: String,
    val role: String,
    @ColumnInfo("permissions_json") val permissionsJson: String? = null,
    val active: Boolean,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
