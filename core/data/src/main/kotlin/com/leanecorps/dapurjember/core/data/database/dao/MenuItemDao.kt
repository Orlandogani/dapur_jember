package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.MenuItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {
    @Upsert
    suspend fun upsert(entity: MenuItemEntity)

    @Upsert
    suspend fun upsertAll(entities: List<MenuItemEntity>)

    @Query("SELECT * FROM menu_item WHERE deleted_at IS NULL ORDER BY sort_order, name")
    fun observeAll(): Flow<List<MenuItemEntity>>

    @Query(
        "SELECT * FROM menu_item WHERE category_id = :categoryId AND deleted_at IS NULL " +
            "ORDER BY sort_order, name",
    )
    fun observeByCategory(categoryId: String): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_item WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): MenuItemEntity?

    @Query("SELECT * FROM menu_item WHERE id = :id AND deleted_at IS NULL")
    fun observeById(id: String): Flow<MenuItemEntity?>

    @Query(
        "UPDATE menu_item SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
