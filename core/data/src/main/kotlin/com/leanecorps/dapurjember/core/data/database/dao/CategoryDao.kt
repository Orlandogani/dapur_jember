package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Upsert
    suspend fun upsert(entity: CategoryEntity)

    @Upsert
    suspend fun upsertAll(entities: List<CategoryEntity>)

    @Query("SELECT * FROM category WHERE deleted_at IS NULL ORDER BY sort_order, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): CategoryEntity?

    @Query(
        "UPDATE category SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
