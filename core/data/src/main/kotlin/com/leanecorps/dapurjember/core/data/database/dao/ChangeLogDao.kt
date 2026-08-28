package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.leanecorps.dapurjember.core.data.database.entity.ChangeLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChangeLogDao {
    @Insert
    suspend fun insert(entity: ChangeLogEntity)

    @Insert
    suspend fun insertAll(entities: List<ChangeLogEntity>)

    @Query("SELECT * FROM change_log WHERE synced_at IS NULL ORDER BY timestamp")
    fun observeUnsynced(): Flow<List<ChangeLogEntity>>

    @Query("SELECT COUNT(*) FROM change_log WHERE synced_at IS NULL")
    suspend fun unsyncedCount(): Int

    @Query("UPDATE change_log SET synced_at = :syncedAt WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, syncedAt: Long)
}
