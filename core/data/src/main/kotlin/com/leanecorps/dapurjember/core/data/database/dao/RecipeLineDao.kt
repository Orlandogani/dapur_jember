package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.RecipeLineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeLineDao {
    /** Strict insert — fails if this (variant, ingredient) pair is already on the recipe. */
    @Insert
    suspend fun insert(entity: RecipeLineEntity)

    @Upsert
    suspend fun upsert(entity: RecipeLineEntity)

    @Upsert
    suspend fun upsertAll(entities: List<RecipeLineEntity>)

    @Query("SELECT * FROM recipe_line WHERE menu_variant_id = :variantId AND deleted_at IS NULL")
    fun observeForVariant(variantId: String): Flow<List<RecipeLineEntity>>

    @Query("SELECT * FROM recipe_line WHERE menu_variant_id = :variantId AND deleted_at IS NULL")
    suspend fun getForVariant(variantId: String): List<RecipeLineEntity>

    @Query(
        "UPDATE recipe_line SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
