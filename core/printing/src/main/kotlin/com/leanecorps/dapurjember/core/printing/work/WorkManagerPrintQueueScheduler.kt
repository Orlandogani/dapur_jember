package com.leanecorps.dapurjember.core.printing.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.leanecorps.dapurjember.core.domain.printing.PrintQueueScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject

private val BACKOFF = Duration.ofSeconds(15)
private val PERIOD = Duration.ofMinutes(15)

internal class WorkManagerPrintQueueScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : PrintQueueScheduler {

    private val workManager get() = WorkManager.getInstance(context)

    override fun drainSoon() {
        val request = OneTimeWorkRequestBuilder<PrintQueueWorker>()
            .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF)
            .build()
        workManager.enqueueUniqueWork(PrintQueueWorker.UNIQUE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    override fun ensurePeriodicDrain() {
        val request = PeriodicWorkRequestBuilder<PrintQueueWorker>(PERIOD)
            .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PrintQueueWorker.PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
