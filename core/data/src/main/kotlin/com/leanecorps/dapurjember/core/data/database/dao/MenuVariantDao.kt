package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.MenuVariantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuVariantDao {
    @Upsert
    suspend fun upsert(entity: MenuVariantEntity)

    @Upsert
    suspend fun upsertAll(entities: List<MenuVariantEntity>)

    @Query(
        "SELECT * FROM menu_variant WHERE menu_item_id = :menuItemId AND deleted_at IS NULL " +
            "ORDER BY sort_order, name",
    )
    fun observeForItem(menuItemId: String): Flow<List<MenuVariantEntity>>

    @Query("SELECT * FROM menu_variant WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): MenuVariantEntity?

    @Query(
        "SELECT * FROM menu_variant WHERE menu_item_id = :menuItemId AND deleted_at IS NULL " +
            "ORDER BY sort_order, name",
    )
    suspend fun getForItem(menuItemId: String): List<MenuVariantEntity>

    @Query(
        "SELECT v.* FROM menu_variant v " +
            "INNER JOIN menu_item i ON i.id = v.menu_item_id " +
            "WHERE i.category_id = :categoryId AND v.deleted_at IS NULL AND i.deleted_at IS NULL " +
            "ORDER BY v.sort_order, v.name",
    )
    fun observeForCategory(categoryId: String): Flow<List<MenuVariantEntity>>

    @Query(
        "UPDATE menu_variant SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
