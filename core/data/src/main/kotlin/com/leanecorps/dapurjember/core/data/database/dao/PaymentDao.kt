package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.leanecorps.dapurjember.core.data.database.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(entity: PaymentEntity)

    @Query("SELECT * FROM payment WHERE order_id = :orderId AND deleted_at IS NULL ORDER BY created_at")
    fun observeForOrder(orderId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payment WHERE order_id = :orderId AND deleted_at IS NULL ORDER BY created_at")
    suspend fun getForOrder(orderId: String): List<PaymentEntity>

    @Query("SELECT COALESCE(SUM(amount_minor), 0) FROM payment WHERE order_id = :orderId AND deleted_at IS NULL")
    suspend fun totalPaidMinor(orderId: String): Long

    @Query(
        "SELECT COALESCE(SUM(p.amount_minor), 0) FROM payment p " +
            "INNER JOIN orders o ON o.id = p.order_id " +
            "WHERE o.shift_id = :shiftId AND p.method = :method " +
            "AND p.deleted_at IS NULL AND o.deleted_at IS NULL",
    )
    suspend fun totalForShiftByMethod(shiftId: String, method: String): Long

    @Query(
        "UPDATE payment SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
