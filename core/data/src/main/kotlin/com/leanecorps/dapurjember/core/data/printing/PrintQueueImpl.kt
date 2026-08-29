package com.leanecorps.dapurjember.core.data.printing

import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.common.time.TimeProvider
import com.leanecorps.dapurjember.core.data.database.dao.PrintJobDao
import com.leanecorps.dapurjember.core.data.database.entity.PrintJobEntity
import com.leanecorps.dapurjember.core.domain.printing.PrintAttemptResult
import com.leanecorps.dapurjember.core.domain.printing.PrintJob
import com.leanecorps.dapurjember.core.domain.printing.PrintJobState
import com.leanecorps.dapurjember.core.domain.printing.PrintJobType
import com.leanecorps.dapurjember.core.domain.printing.PrintQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal fun PrintJobEntity.toDomain() = PrintJob(
    id = id,
    type = runCatching { PrintJobType.valueOf(type) }.getOrDefault(PrintJobType.RECEIPT),
    payload = payloadBytes,
    targetPrinterId = targetPrinterId,
    state = runCatching { PrintJobState.valueOf(state) }.getOrDefault(PrintJobState.PENDING),
    attempts = attempts,
    lastError = lastError,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private const val MAX_ATTEMPTS = 5

internal class PrintQueueImpl @Inject constructor(
    private val dao: PrintJobDao,
    private val time: TimeProvider,
) : PrintQueue {

    override suspend fun enqueue(type: PrintJobType, payload: ByteArray, targetPrinterId: String?): String {
        val now = time.nowMillis()
        val id = UuidV7.generate()
        dao.insert(
            PrintJobEntity(
                id = id,
                type = type.name,
                payloadBytes = payload,
                targetPrinterId = targetPrinterId,
                state = PrintJobState.PENDING.name,
                attempts = 0,
                lastError = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    override suspend fun drainable(): List<PrintJob> = dao.getDrainable(MAX_ATTEMPTS).map { it.toDomain() }

    override suspend fun markPrinting(jobId: String) {
        val job = dao.getById(jobId) ?: return
        dao.updateState(jobId, PrintJobState.PRINTING.name, job.attempts, job.lastError, time.nowMillis())
    }

    override suspend fun recordResult(jobId: String, result: PrintAttemptResult) {
        val job = dao.getById(jobId) ?: return
        val attempts = job.attempts + 1
        val (state, error) = when (result) {
            is PrintAttemptResult.Success -> PrintJobState.DONE to null
            is PrintAttemptResult.Failure -> PrintJobState.FAILED to result.error
        }
        dao.updateState(jobId, state.name, attempts, error, time.nowMillis())
    }

    override fun observeJobs(): Flow<List<PrintJob>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    override suspend fun prune(olderThanMillis: Long) = dao.prunePrinted(olderThanMillis)
}
