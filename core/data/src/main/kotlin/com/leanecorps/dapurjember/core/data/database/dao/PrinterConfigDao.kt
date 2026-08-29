package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.leanecorps.dapurjember.core.data.database.entity.PrinterConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrinterConfigDao {

    @Upsert
    suspend fun upsert(config: PrinterConfigEntity)

    @Query("SELECT * FROM printer_config WHERE deleted_at IS NULL ORDER BY name")
    fun observeAll(): Flow<List<PrinterConfigEntity>>

    @Query("SELECT * FROM printer_config WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): PrinterConfigEntity?

    /** Every live printer assigned [role] (roles is a comma-separated list). */
    @Query(
        "SELECT * FROM printer_config WHERE deleted_at IS NULL AND " +
            "(',' || replace(roles, ' ', '') || ',') LIKE '%,' || :role || ',%'",
    )
    suspend fun forRole(role: String): List<PrinterConfigEntity>

    @Query("UPDATE printer_config SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)
}
