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

    /**
     * Every link row for the item, soft-deleted ones included — the `(menu_item_id,
     * modifier_group_id)` unique index still holds after a soft delete, so a re-attach must
     * restore the old row rather than insert a new one.
     */
    @Query("SELECT * FROM item_modifier_group WHERE menu_item_id = :menuItemId")
    suspend fun getAllForItem(menuItemId: String): List<ItemModifierGroupEntity>

    @Query(
        "UPDATE item_modifier_group SET deleted_at = NULL, sort_order = :sortOrder, " +
            "updated_at = :now, revision = revision + 1 WHERE id = :id",
    )
    suspend fun restore(id: String, sortOrder: Int, now: Long)

    @Query(
        "UPDATE item_modifier_group SET sort_order = :sortOrder, updated_at = :now, " +
            "revision = revision + 1 WHERE id = :id",
    )
    suspend fun reorder(id: String, sortOrder: Int, now: Long)

    @Query(
        "UPDATE item_modifier_group SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
