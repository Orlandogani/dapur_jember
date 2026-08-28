package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.ItemModifierGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemModifierGroupDao {
    /** Strict insert — fails if the (menu_item_id, modifier_group_id) pair is already linked. */
    @Insert
    suspend fun insert(entity: ItemModifierGroupEntity)

    @Upsert
    suspend fun upsert(entity: ItemModifierGroupEntity)

    @Query(
        "SELECT * FROM item_modifier_group WHERE menu_item_id = :menuItemId " +
            "AND deleted_at IS NULL ORDER BY sort_order",
    )
    fun observeForItem(menuItemId: String): Flow<List<ItemModifierGroupEntity>>

    @Query(
        "UPDATE item_modifier_group SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
