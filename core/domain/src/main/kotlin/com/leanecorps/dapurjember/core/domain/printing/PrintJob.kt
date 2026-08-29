package com.leanecorps.dapurjember.core.domain.printing

import kotlinx.coroutines.flow.Flow

enum class PrintJobType { KITCHEN, BAR, RECEIPT, ZREPORT }

enum class PrintJobState { PENDING, PRINTING, DONE, FAILED }

/**
 * A rendered print job waiting for a printer. [payload] is complete ESC/POS bytes — the
 * transport just writes them. The queue exists so a dead printer never rolls back a sale
 * (FR-PR3): the sale commits, the job retries here.
 */
data class PrintJob(
    val id: String,
    val type: PrintJobType,
    val payload: ByteArray,
    val targetPrinterId: String?,
    val state: PrintJobState,
    val attempts: Int,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Outcome of one attempt to send a job to a printer. */
sealed interface PrintAttemptResult {
    data object Success : PrintAttemptResult

    data class Failure(val error: String) : PrintAttemptResult
}

/**
 * The Room-backed print queue. [enqueue] must run in the same transaction as the mutation
 * that triggered the print (architecture §6) so a crash between sale-commit and job-insert is
 * impossible. Draining is done by a WorkManager worker, never on the sale's thread.
 */
interface PrintQueue {

    /** Adds a job in PENDING state; returns its id. */
    suspend fun enqueue(type: PrintJobType, payload: ByteArray, targetPrinterId: String? = null): String

    /** PENDING + FAILED jobs, oldest first. */
    suspend fun drainable(): List<PrintJob>

    suspend fun markPrinting(jobId: String)

    suspend fun recordResult(jobId: String, result: PrintAttemptResult)

    fun observeJobs(): Flow<List<PrintJob>>

    /** Count of jobs still needing a printer — drives the "N queued" banner (flows doc §5). */
    fun observePendingCount(): Flow<Int>

    /** Drops DONE jobs older than [olderThanMillis]. */
    suspend fun prune(olderThanMillis: Long)
}
