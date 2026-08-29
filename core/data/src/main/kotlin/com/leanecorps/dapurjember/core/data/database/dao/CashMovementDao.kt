package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.leanecorps.dapurjember.core.data.database.entity.CashMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashMovementDao {
    @Insert
    suspend fun insert(entity: CashMovementEntity)

    @Query(
        "SELECT * FROM cash_movement WHERE shift_id = :shiftId AND deleted_at IS NULL " +
            "ORDER BY created_at",
    )
    fun observeForShift(shiftId: String): Flow<List<CashMovementEntity>>

    @Query(
        "UPDATE cash_movement SET deleted_at = :deletedAt, updated_at = :deletedAt, " +
            "revision = revision + 1 WHERE id = :id AND deleted_at IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long)
}
