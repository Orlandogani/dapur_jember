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
}
