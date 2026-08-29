package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.leanecorps.dapurjember.core.data.database.entity.OrderLineModifierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderLineModifierDao {
    @Insert
    suspend fun insert(entity: OrderLineModifierEntity)

    @Insert
    suspend fun insertAll(entities: List<OrderLineModifierEntity>)

    @Query(
        "SELECT * FROM order_line_modifier WHERE order_line_id = :lineId AND deleted_at IS NULL " +
            "ORDER BY name_snapshot",
    )
    fun observeForLine(lineId: String): Flow<List<OrderLineModifierEntity>>

    @Query("SELECT * FROM order_line_modifier WHERE order_line_id = :lineId AND deleted_at IS NULL")
    suspend fun getForLine(lineId: String): List<OrderLineModifierEntity>

    @Query(
        "SELECT olm.* FROM order_line_modifier olm " +
            "INNER JOIN order_line ol ON ol.id = olm.order_line_id " +
            "WHERE ol.order_id = :orderId AND olm.deleted_at IS NULL AND ol.deleted_at IS NULL",
    )
    fun observeForOrder(orderId: String): Flow<List<OrderLineModifierEntity>>
}
