package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.FloorAreaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FloorAreaDao {
    @Upsert
    suspend fun upsert(entity: FloorAreaEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FloorAreaEntity>)

    @Query("SELECT * FROM floor_area WHERE deleted_at IS NULL ORDER BY sort_order, name")
    fun observeAll(): Flow<List<FloorAreaEntity>>

    @Query("SELECT * FROM floor_area WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): FloorAreaEntity?

    @Query(
        "UPDATE floor_area SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
