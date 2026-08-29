package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    @Upsert
    suspend fun upsert(entity: IngredientEntity)

    @Query("SELECT * FROM ingredient WHERE deleted_at IS NULL ORDER BY name")
    fun observeAll(): Flow<List<IngredientEntity>>

    /** FR-I7 — ingredients at or below their low-stock threshold. */
    @Query(
        "SELECT * FROM ingredient WHERE deleted_at IS NULL " +
            "AND current_stock_base <= low_stock_threshold_base ORDER BY name",
    )
    fun observeLowStock(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredient WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): IngredientEntity?

    @Query(
        "UPDATE ingredient SET current_stock_base = :stockBase, avg_cost_per_base_minor = :avgCostMinor, " +
            "updated_at = :updatedAt, revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun updateStock(id: String, stockBase: Double, avgCostMinor: Long, updatedAt: Long)

    @Query(
        "UPDATE ingredient SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
