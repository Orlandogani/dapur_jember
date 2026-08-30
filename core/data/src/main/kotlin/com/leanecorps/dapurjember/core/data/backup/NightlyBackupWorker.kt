package com.leanecorps.dapurjember.core.data.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.leanecorps.dapurjember.core.domain.backup.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject

/**
 * Takes the automatic daily backup and trims the rolling window to the last seven (FR-D3).
 * Failures retry rather than being swallowed — a backup that quietly stops running is worse
 * than no backup, because the owner believes they are covered.
 */
@HiltWorker
class NightlyBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val backups: BackupRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        backups.createAutomaticBackup()
        backups.pruneOldBackups()
    }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })

    companion object {
        const val UNIQUE_WORK = "nightly-backup"
    }
}

/** Registers the nightly backup once per process start. */
class BackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureScheduled() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NightlyBackupWorker.UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NightlyBackupWorker>(Duration.ofDays(1)).build(),
        )
    }
}
