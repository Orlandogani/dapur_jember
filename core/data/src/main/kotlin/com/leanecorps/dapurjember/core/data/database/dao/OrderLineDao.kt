package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.OrderLineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderLineDao {
    @Upsert
    suspend fun upsert(entity: OrderLineEntity)

    @Upsert
    suspend fun upsertAll(entities: List<OrderLineEntity>)

    @Query(
        "SELECT * FROM order_line WHERE order_id = :orderId AND deleted_at IS NULL " +
            "ORDER BY course, created_at",
    )
    fun observeForOrder(orderId: String): Flow<List<OrderLineEntity>>

    @Query(
        "SELECT * FROM order_line WHERE order_id = :orderId AND deleted_at IS NULL " +
            "ORDER BY course, created_at",
    )
    suspend fun getForOrder(orderId: String): List<OrderLineEntity>

    @Query("SELECT * FROM order_line WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): OrderLineEntity?

    /** FR-O3: lines not yet printed to the kitchen. `send` prints exactly these. */
    @Query(
        "SELECT * FROM order_line WHERE order_id = :orderId AND sent_at IS NULL " +
            "AND state = 'ACTIVE' AND deleted_at IS NULL ORDER BY course, created_at",
    )
    suspend fun getUnsent(orderId: String): List<OrderLineEntity>

    @Query(
        "UPDATE order_line SET sent_at = :sentAt, updated_at = :sentAt, revision = revision + 1 " +
            "WHERE id IN (:ids) AND sent_at IS NULL",
    )
    suspend fun markSent(ids: List<String>, sentAt: Long)

    @Query(
        "UPDATE order_line SET state = 'VOIDED', void_reason = :reason, updated_at = :updatedAt, " +
            "revision = revision + 1 WHERE id = :id AND state = 'ACTIVE' AND deleted_at IS NULL",
    )
    suspend fun voidLine(id: String, reason: String, updatedAt: Long)

    @Query(
        "UPDATE order_line SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
