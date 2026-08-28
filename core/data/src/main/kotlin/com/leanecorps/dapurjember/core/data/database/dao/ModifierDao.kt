package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.ModifierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModifierDao {
    @Upsert
    suspend fun upsert(entity: ModifierEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ModifierEntity>)

    @Query(
        "SELECT * FROM modifier WHERE modifier_group_id = :groupId AND deleted_at IS NULL " +
            "ORDER BY sort_order, name",
    )
    fun observeForGroup(groupId: String): Flow<List<ModifierEntity>>

    @Query("SELECT * FROM modifier WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): ModifierEntity?

    @Query(
        "UPDATE modifier SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
