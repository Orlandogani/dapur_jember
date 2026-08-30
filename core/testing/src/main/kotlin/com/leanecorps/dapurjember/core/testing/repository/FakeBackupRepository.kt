package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.domain.backup.BackupFile
import com.leanecorps.dapurjember.core.domain.backup.BackupRepository
import com.leanecorps.dapurjember.core.domain.backup.RestoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [BackupRepository]; [restorePassphrase] decides which passphrase "works". */
class FakeBackupRepository(
    var restorePassphrase: String = "hunter2hunter2",
) : BackupRepository {

    private val files = MutableStateFlow<List<BackupFile>>(emptyList())
    private var nextId = 0
    var lastCreatedWith: String? = null
        private set

    override fun observeBackups(): Flow<List<BackupFile>> = files

    override suspend fun lastBackupAt(): Long? = files.value.maxOfOrNull { it.createdAt }

    override suspend fun createBackup(passphrase: CharArray): BackupFile {
        lastCreatedWith = String(passphrase)
        val file = BackupFile(
            name = "backup-${++nextId}.djbk",
            path = "/tmp/backup-$nextId.djbk",
            sizeBytes = 1_024,
            createdAt = nextId.toLong(),
        )
        files.update { it + file }
        return file
    }

    override suspend fun restore(file: BackupFile, passphrase: CharArray): RestoreResult =
        if (String(passphrase) == restorePassphrase) {
            RestoreResult.RestartRequired
        } else {
            RestoreResult.Failed("Wrong passphrase, or the backup file is damaged.")
        }

    override suspend fun pruneOldBackups(keep: Int) =
        files.update { list -> list.sortedByDescending { it.createdAt }.take(keep) }
}
