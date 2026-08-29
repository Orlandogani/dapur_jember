package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Upsert
    suspend fun upsert(entity: OrderEntity)

    @Query("SELECT * FROM orders WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE id = :id AND deleted_at IS NULL")
    fun observeById(id: String): Flow<OrderEntity?>

    @Query(
        "SELECT * FROM orders WHERE dining_table_id = :tableId AND deleted_at IS NULL " +
            "AND state NOT IN ('CLOSED', 'VOIDED') ORDER BY opened_at DESC LIMIT 1",
    )
    fun observeActiveForTable(tableId: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE business_day = :businessDay AND deleted_at IS NULL ORDER BY opened_at")
    fun observeForBusinessDay(businessDay: String): Flow<List<OrderEntity>>

    @Query(
        "UPDATE orders SET state = :state, updated_at = :updatedAt, revision = revision + 1 " +
            "WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun updateState(id: String, state: String, updatedAt: Long)
}
