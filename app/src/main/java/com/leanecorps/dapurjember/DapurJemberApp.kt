package com.leanecorps.dapurjember

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.leanecorps.dapurjember.core.data.backup.BackupScheduler
import com.leanecorps.dapurjember.core.domain.printing.PrintQueueScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DapurJemberApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var printQueueScheduler: PrintQueueScheduler

    @Inject
    lateinit var backupScheduler: BackupScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        printQueueScheduler.ensurePeriodicDrain()
        backupScheduler.ensureScheduled()
    }
}
