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

    @Query(
        "SELECT * FROM modifier WHERE modifier_group_id = :groupId AND deleted_at IS NULL " +
            "ORDER BY sort_order, name",
    )
    suspend fun getForGroup(groupId: String): List<ModifierEntity>

    @Query(
        "SELECT m.* FROM modifier m " +
            "INNER JOIN item_modifier_group img ON img.modifier_group_id = m.modifier_group_id " +
            "WHERE img.menu_item_id = :menuItemId AND img.deleted_at IS NULL AND m.deleted_at IS NULL " +
            "ORDER BY m.sort_order, m.name",
    )
    fun observeForItem(menuItemId: String): Flow<List<ModifierEntity>>

    @Query("SELECT * FROM modifier WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): ModifierEntity?

    @Query(
        "UPDATE modifier SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
