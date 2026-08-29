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
}
