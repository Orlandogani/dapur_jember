package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Upsert
    suspend fun upsert(entity: ShiftEntity)

    /** The single open shift, if any (`closed_at IS NULL`). */
    @Query("SELECT * FROM shift WHERE closed_at IS NULL AND deleted_at IS NULL ORDER BY opened_at DESC LIMIT 1")
    fun observeOpenShift(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shift WHERE business_day = :businessDay AND deleted_at IS NULL ORDER BY opened_at")
    fun observeForBusinessDay(businessDay: String): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shift WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): ShiftEntity?

    @Query(
        "UPDATE shift SET closed_at = :closedAt, closed_by = :closedBy, counted_cash_minor = :countedCash, " +
            "expected_cash_minor = :expectedCash, variance_minor = :variance, updated_at = :closedAt, " +
            "revision = revision + 1 WHERE id = :id AND closed_at IS NULL",
    )
    suspend fun close(
        id: String,
        closedAt: Long,
        closedBy: String,
        countedCash: Long,
        expectedCash: Long,
        variance: Long,
    )
}
