package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.leanecorps.dapurjember.core.data.database.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Insert
    suspend fun insert(entity: StockMovementEntity)

    @Insert
    suspend fun insertAll(entities: List<StockMovementEntity>)

    @Query(
        "SELECT * FROM stock_movement WHERE ingredient_id = :ingredientId AND deleted_at IS NULL " +
            "ORDER BY created_at DESC",
    )
    fun observeForIngredient(ingredientId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movement WHERE order_line_id = :orderLineId AND deleted_at IS NULL")
    suspend fun getForOrderLine(orderLineId: String): List<StockMovementEntity>
}
