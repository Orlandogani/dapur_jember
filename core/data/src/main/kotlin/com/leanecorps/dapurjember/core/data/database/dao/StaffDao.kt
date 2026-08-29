package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Upsert
    suspend fun upsert(entity: StaffEntity)

    @Query("SELECT * FROM staff WHERE deleted_at IS NULL ORDER BY name")
    fun observeAll(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE active = 1 AND deleted_at IS NULL ORDER BY name")
    fun observeActive(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): StaffEntity?

    @Query(
        "UPDATE staff SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
