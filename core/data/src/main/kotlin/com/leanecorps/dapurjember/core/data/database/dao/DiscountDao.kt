package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.leanecorps.dapurjember.core.data.database.entity.DiscountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscountDao {
    @Insert
    suspend fun insert(entity: DiscountEntity)

    @Query("SELECT * FROM discount WHERE order_id = :orderId AND deleted_at IS NULL ORDER BY created_at")
    fun observeForOrder(orderId: String): Flow<List<DiscountEntity>>

    @Query("SELECT * FROM discount WHERE order_line_id = :lineId AND deleted_at IS NULL ORDER BY created_at")
    fun observeForLine(lineId: String): Flow<List<DiscountEntity>>

    @Query(
        "UPDATE discount SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
