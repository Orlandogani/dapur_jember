package com.leanecorps.dapurjember.core.domain.backup

import kotlinx.coroutines.flow.Flow

/**
 * Backup and restore (FR-D1..D4). Risk R1 in the PRD — a lost, stolen or bricked tablet must
 * not mean a lost year of sales — so this is a first-class v1 feature, not a nice-to-have.
 */
interface BackupRepository {

    fun observeBackups(): Flow<List<BackupFile>>

    /** Epoch millis of the newest backup, or null if the restaurant has never taken one. */
    suspend fun lastBackupAt(): Long?

    /**
     * Writes an encrypted backup to local storage and returns it. The passphrase is never
     * stored — an owner who loses it cannot restore, and the setup wizard says so (FR-A5).
     */
    suspend fun createBackup(passphrase: CharArray): BackupFile

    /**
     * Replaces the live database with [file]'s contents (FR-D2). Destructive: the caller must
     * have confirmed explicitly. The app must be restarted afterwards — [RestoreResult] says so.
     */
    suspend fun restore(file: BackupFile, passphrase: CharArray): RestoreResult

    /** Keeps the newest [keep] backups and deletes the rest (FR-D3, rolling window). */
    suspend fun pruneOldBackups(keep: Int = DEFAULT_BACKUPS_KEPT)

    companion object {
        /** FR-D3: "retaining the last 7 copies, on a rolling basis". */
        const val DEFAULT_BACKUPS_KEPT: Int = 7

        /** FR-D4: prompt for a backup on every 7th consecutive day without one. */
        const val BACKUP_NAG_DAYS: Int = 7
    }
}

data class BackupFile(val name: String, val path: String, val sizeBytes: Long, val createdAt: Long)

sealed interface RestoreResult {
    /** The database was replaced; the process must restart to reopen it cleanly. */
    data object RestartRequired : RestoreResult

    data class Failed(val message: String) : RestoreResult
}
