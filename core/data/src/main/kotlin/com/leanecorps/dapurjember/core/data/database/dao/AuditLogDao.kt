package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.leanecorps.dapurjember.core.data.database.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insert(entity: AuditLogEntity)

    @Query("SELECT * FROM audit_log ORDER BY created_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AuditLogEntity>>

    @Query(
        "SELECT * FROM audit_log WHERE entity_type = :entityType AND entity_id = :entityId " +
            "ORDER BY created_at DESC",
    )
    fun observeForEntity(entityType: String, entityId: String): Flow<List<AuditLogEntity>>
}
