package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.ModifierGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModifierGroupDao {
    @Upsert
    suspend fun upsert(entity: ModifierGroupEntity)

    @Query("SELECT * FROM modifier_group WHERE deleted_at IS NULL ORDER BY name")
    fun observeAll(): Flow<List<ModifierGroupEntity>>

    @Query(
        "SELECT g.* FROM modifier_group g " +
            "INNER JOIN item_modifier_group img ON img.modifier_group_id = g.id " +
            "WHERE img.menu_item_id = :menuItemId AND img.deleted_at IS NULL " +
            "AND g.deleted_at IS NULL ORDER BY img.sort_order",
    )
    fun observeForItem(menuItemId: String): Flow<List<ModifierGroupEntity>>

    @Query("SELECT * FROM modifier_group WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): ModifierGroupEntity?

    @Query(
        "UPDATE modifier_group SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
