package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Upsert
    suspend fun upsert(entity: SupplierEntity)

    @Query("SELECT * FROM supplier WHERE deleted_at IS NULL ORDER BY name")
    fun observeAll(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM supplier WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): SupplierEntity?

    @Query(
        "UPDATE supplier SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
