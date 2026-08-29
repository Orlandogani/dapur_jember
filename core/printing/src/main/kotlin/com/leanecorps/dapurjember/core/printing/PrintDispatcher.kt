package com.leanecorps.dapurjember.core.printing

import com.leanecorps.dapurjember.core.domain.printing.PrintAttemptResult
import com.leanecorps.dapurjember.core.domain.printing.PrintJob
import com.leanecorps.dapurjember.core.domain.printing.PrintJobType
import com.leanecorps.dapurjember.core.domain.printing.PrintQueue
import com.leanecorps.dapurjember.core.domain.printing.Printer
import com.leanecorps.dapurjember.core.domain.printing.PrinterRepository
import com.leanecorps.dapurjember.core.domain.printing.PrinterRole
import com.leanecorps.dapurjember.core.printing.transport.PrinterTransportFactory
import javax.inject.Inject

/** Outcome of one drain pass — [retryNeeded] tells the WorkManager worker whether to reschedule. */
data class DrainOutcome(val printed: Int, val failed: Int, val retryNeeded: Boolean)

/**
 * Works through the [PrintQueue] once: for each drainable job, resolve its printer, open a
 * transport, and write the bytes. A job with no printer configured is left PENDING (it prints
 * as soon as a printer is added); a job that errors is marked FAILED and retried later. The
 * sale that produced the job has long since committed — nothing here can roll it back.
 */
class PrintDispatcher @Inject constructor(
    private val queue: PrintQueue,
    private val printers: PrinterRepository,
    private val transports: PrinterTransportFactory,
) {

    suspend fun drainOnce(): DrainOutcome {
        var printed = 0
        var failed = 0
        var noPrinter = false

        for (job in queue.drainable()) {
            val printer = resolvePrinter(job)
            if (printer == null) {
                noPrinter = true
                continue
            }
            when (attempt(job, printer)) {
                is PrintAttemptResult.Success -> printed++
                is PrintAttemptResult.Failure -> failed++
            }
        }
        return DrainOutcome(printed = printed, failed = failed, retryNeeded = failed > 0 || noPrinter)
    }

    private suspend fun attempt(job: PrintJob, printer: Printer): PrintAttemptResult {
        queue.markPrinting(job.id)
        val result = runCatching { transports.create(printer).send(job.payload) }
            .fold(
                onSuccess = { PrintAttemptResult.Success },
                onFailure = { PrintAttemptResult.Failure(it.message ?: "print failed") },
            )
        queue.recordResult(job.id, result)
        return result
    }

    private suspend fun resolvePrinter(job: PrintJob): Printer? {
        job.targetPrinterId?.let { return printers.getPrinter(it) }
        return printers.printersForRole(roleFor(job.type)).firstOrNull()
    }

    private fun roleFor(type: PrintJobType): PrinterRole = when (type) {
        PrintJobType.KITCHEN -> PrinterRole.KITCHEN
        PrintJobType.BAR -> PrinterRole.BAR
        PrintJobType.RECEIPT, PrintJobType.ZREPORT -> PrinterRole.RECEIPT
    }
}
