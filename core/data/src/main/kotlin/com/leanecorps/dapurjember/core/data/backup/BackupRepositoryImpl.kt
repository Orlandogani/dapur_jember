package com.leanecorps.dapurjember.core.data.backup

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import com.leanecorps.dapurjember.core.common.crypto.BackupCrypto
import com.leanecorps.dapurjember.core.common.time.TimeProvider
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.domain.backup.BackupFile
import com.leanecorps.dapurjember.core.domain.backup.BackupRepository
import com.leanecorps.dapurjember.core.domain.backup.RestoreResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private const val BACKUP_DIR = "backups"
private const val BACKUP_EXTENSION = ".djbk"
private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)

/**
 * Local encrypted backups (FR-D1..D3).
 *
 * The live database is encrypted with a key that lives in the Android Keystore and never
 * leaves the device, so a copy of the raw file would be useless on a replacement tablet. The
 * backup is therefore re-encrypted under the owner's passphrase via [BackupCrypto].
 *
 * Before reading the file we run `wal_checkpoint(FULL)`, otherwise recent transactions would
 * still be sitting in the `-wal` sidecar and the backup would silently miss the last orders —
 * exactly the data an owner would most want back.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: DapurJemberDatabase,
    private val time: TimeProvider,
) : BackupRepository {

    private val backups = MutableStateFlow(emptyList<BackupFile>())

    private val directory: File
        get() = File(context.filesDir, BACKUP_DIR).apply { mkdirs() }

    init {
        refresh()
    }

    override fun observeBackups(): Flow<List<BackupFile>> = backups.asStateFlow()

    override suspend fun lastBackupAt(): Long? = listBackups().maxOfOrNull { it.createdAt }

    override suspend fun createBackup(passphrase: CharArray): BackupFile {
        checkpointWal()
        val dbFile = context.getDatabasePath(DapurJemberDatabase.NAME)
        val encrypted = BackupCrypto.encrypt(dbFile.readBytes(), passphrase)

        val now = time.nowMillis()
        val target = File(directory, "dapurjember-${STAMP.format(Instant.ofEpochMilli(now))}$BACKUP_EXTENSION")
        target.writeBytes(encrypted)
        refresh()
        return target.toBackupFile()
    }

    override suspend fun restore(file: BackupFile, passphrase: CharArray): RestoreResult {
        val plaintext = readBackup(file, passphrase).getOrElse { e ->
            return RestoreResult.Failed(e.message ?: "The backup could not be read.")
        }
        return swapDatabase(plaintext)
    }

    private fun readBackup(file: BackupFile, passphrase: CharArray): Result<ByteArray> {
        val source = File(file.path)
        if (!source.exists()) return Result.failure(NoSuchFileException(source, reason = "backup missing"))
        return runCatching { BackupCrypto.decrypt(source.readBytes(), passphrase) }
    }

    private fun swapDatabase(plaintext: ByteArray): RestoreResult {
        // Take a safety copy first: if the swap dies half-way, the old database is recoverable
        // rather than lost — a half-restored POS is worse than a failed restore (§6 policy).
        val dbFile = context.getDatabasePath(DapurJemberDatabase.NAME)
        val rollback = File(dbFile.parentFile, "${DapurJemberDatabase.NAME}.pre-restore")
        checkpointWal()
        if (dbFile.exists()) dbFile.copyTo(rollback, overwrite = true)

        return try {
            database.close()
            dbFile.writeBytes(plaintext)
            // The old -wal/-shm describe the replaced file and would corrupt the new one.
            deleteSidecars(dbFile)
            RestoreResult.RestartRequired
        } catch (e: IOException) {
            runCatching { rollback.copyTo(dbFile, overwrite = true) }
            RestoreResult.Failed("Restore failed and the previous data was kept: ${e.message}")
        }
    }

    override suspend fun pruneOldBackups(keep: Int) {
        listBackups().drop(keep).forEach { File(it.path).delete() }
        refresh()
    }

    /**
     * Flushes the `-wal` sidecar into the main database file. The cursor must actually be
     * *read* — Android's SQLite cursors are lazy, and merely closing one never runs the
     * statement, which would silently produce a backup missing the most recent orders.
     */
    private fun checkpointWal() {
        database.openHelper.writableDatabase
            .query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(TRUNCATE)"))
            .use { it.moveToFirst() }
    }

    private fun deleteSidecars(dbFile: File) {
        File("${dbFile.path}-wal").delete()
        File("${dbFile.path}-shm").delete()
    }

    /** Newest first. */
    private fun listBackups(): List<BackupFile> =
        directory.listFiles { f -> f.isFile && f.name.endsWith(BACKUP_EXTENSION) }
            .orEmpty()
            .map { it.toBackupFile() }
            .sortedByDescending { it.createdAt }

    private fun refresh() {
        backups.value = listBackups()
    }

    /**
     * The creation time comes from the timestamp in the file name, not the filesystem mtime:
     * copying a backup off the tablet and back would otherwise reorder the rolling window.
     */
    private fun File.toBackupFile() = BackupFile(
        name = name,
        path = path,
        sizeBytes = length(),
        createdAt = createdAtFromName(name) ?: lastModified(),
    )

    private fun createdAtFromName(name: String): Long? {
        val stamp = name.removePrefix("dapurjember-").removeSuffix(BACKUP_EXTENSION)
        return runCatching {
            LocalDateTime.parse(stamp, STAMP).toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrNull()
    }
}
