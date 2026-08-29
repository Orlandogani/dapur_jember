package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.leanecorps.dapurjember.core.data.database.entity.PrintJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintJobDao {

    @Insert
    suspend fun insert(job: PrintJobEntity)

    @Query("SELECT * FROM print_job WHERE id = :id")
    suspend fun getById(id: String): PrintJobEntity?

    /**
     * Jobs still needing a printer, oldest first — what the queue drainer works through.
     * A job that has burned through [maxAttempts] retries drops out (it stays FAILED for the
     * banner and manual retry, but auto-draining stops hammering a broken printer).
     */
    @Query(
        "SELECT * FROM print_job WHERE state IN ('PENDING', 'FAILED') AND attempts < :maxAttempts " +
            "ORDER BY created_at",
    )
    suspend fun getDrainable(maxAttempts: Int): List<PrintJobEntity>

    @Query("SELECT * FROM print_job ORDER BY created_at DESC")
    fun observeAll(): Flow<List<PrintJobEntity>>

    @Query("SELECT COUNT(*) FROM print_job WHERE state IN ('PENDING', 'FAILED')")
    fun observePendingCount(): Flow<Int>

    @Query(
        "UPDATE print_job SET state = :state, attempts = :attempts, last_error = :lastError, " +
            "updated_at = :updatedAt WHERE id = :id",
    )
    suspend fun updateState(id: String, state: String, attempts: Int, lastError: String?, updatedAt: Long)

    @Query("DELETE FROM print_job WHERE state = 'DONE' AND updated_at < :before")
    suspend fun prunePrinted(before: Long)
}
