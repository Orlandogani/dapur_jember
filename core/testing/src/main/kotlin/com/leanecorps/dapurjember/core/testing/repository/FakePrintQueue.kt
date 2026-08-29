package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.domain.printing.PrintAttemptResult
import com.leanecorps.dapurjember.core.domain.printing.PrintJob
import com.leanecorps.dapurjember.core.domain.printing.PrintJobState
import com.leanecorps.dapurjember.core.domain.printing.PrintJobType
import com.leanecorps.dapurjember.core.domain.printing.PrintQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/** In-memory [PrintQueue] for tests. Jobs keep insertion order; [enqueued] is the raw log. */
class FakePrintQueue : PrintQueue {

    private val jobs = MutableStateFlow<List<PrintJob>>(emptyList())
    private val ids = AtomicLong(0)

    val enqueued: List<PrintJob> get() = jobs.value

    override suspend fun enqueue(type: PrintJobType, payload: ByteArray, targetPrinterId: String?): String {
        val id = "job-${ids.incrementAndGet()}"
        jobs.update {
            it + PrintJob(
                id = id,
                type = type,
                payload = payload,
                targetPrinterId = targetPrinterId,
                state = PrintJobState.PENDING,
                attempts = 0,
                lastError = null,
                createdAt = 0,
                updatedAt = 0,
            )
        }
        return id
    }

    override suspend fun drainable(): List<PrintJob> =
        jobs.value.filter { it.state == PrintJobState.PENDING || it.state == PrintJobState.FAILED }

    override suspend fun markPrinting(jobId: String) = mutate(jobId) { it.copy(state = PrintJobState.PRINTING) }

    override suspend fun recordResult(jobId: String, result: PrintAttemptResult) = mutate(jobId) { job ->
        when (result) {
            is PrintAttemptResult.Success -> job.copy(state = PrintJobState.DONE, attempts = job.attempts + 1)
            is PrintAttemptResult.Failure ->
                job.copy(state = PrintJobState.FAILED, attempts = job.attempts + 1, lastError = result.error)
        }
    }

    override fun observeJobs(): Flow<List<PrintJob>> = jobs

    override fun observePendingCount(): Flow<Int> = jobs.map { list ->
        list.count { it.state == PrintJobState.PENDING || it.state == PrintJobState.FAILED }
    }

    override suspend fun prune(olderThanMillis: Long) =
        jobs.update { list -> list.filterNot { it.state == PrintJobState.DONE } }

    private inline fun mutate(jobId: String, transform: (PrintJob) -> PrintJob) =
        jobs.update { list -> list.map { if (it.id == jobId) transform(it) else it } }
}
