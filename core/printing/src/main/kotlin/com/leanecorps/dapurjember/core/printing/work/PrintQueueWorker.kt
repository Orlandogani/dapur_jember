package com.leanecorps.dapurjember.core.printing.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.leanecorps.dapurjember.core.printing.PrintDispatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Drains the print queue once. Returns [Result.retry] while any job still needs a printer or
 * failed to send, so WorkManager backs off and tries again — the sale is long committed, so
 * retrying forever is safe and correct (FR-PR3).
 */
@HiltWorker
class PrintQueueWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dispatcher: PrintDispatcher,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val outcome = runCatching { dispatcher.drainOnce() }.getOrElse { return Result.retry() }
        return if (outcome.retryNeeded) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_WORK = "print-queue-drain"
        const val PERIODIC_WORK = "print-queue-drain-periodic"
    }
}
