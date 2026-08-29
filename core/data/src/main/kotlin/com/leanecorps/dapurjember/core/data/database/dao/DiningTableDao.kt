package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.DiningTableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiningTableDao {
    @Upsert
    suspend fun upsert(entity: DiningTableEntity)

    @Upsert
    suspend fun upsertAll(entities: List<DiningTableEntity>)

    @Query("SELECT * FROM dining_table WHERE deleted_at IS NULL ORDER BY label")
    fun observeAll(): Flow<List<DiningTableEntity>>

    @Query(
        "SELECT * FROM dining_table WHERE floor_area_id = :areaId AND deleted_at IS NULL " +
            "ORDER BY label",
    )
    fun observeByArea(areaId: String): Flow<List<DiningTableEntity>>

    @Query("SELECT * FROM dining_table WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): DiningTableEntity?

    @Query(
        "UPDATE dining_table SET state = :state, updated_at = :updatedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun updateState(id: String, state: String, updatedAt: Long)

    @Query(
        "UPDATE dining_table SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
