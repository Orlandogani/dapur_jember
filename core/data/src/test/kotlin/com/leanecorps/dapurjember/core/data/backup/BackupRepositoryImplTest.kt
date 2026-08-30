package com.leanecorps.dapurjember.core.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.domain.backup.RestoreResult
import com.leanecorps.dapurjember.core.testing.FakeTimeProvider
import com.leanecorps.dapurjember.core.testing.database.MenuEntityFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Uses a real on-disk Room database (not in-memory) because the whole point is copying the
 * database *file*. Robolectric gives us a plain-SQLite build of it; SQLCipher is exercised
 * only on-device, which is noted as a TODO in `DatabaseModule`.
 */
@RunWith(RobolectricTestRunner::class)
class BackupRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var db: DapurJemberDatabase
    private lateinit var repo: BackupRepositoryImpl
    private val time = FakeTimeProvider(now = 1_000L)

    private val passphrase = "hunter2hunter2".toCharArray()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.databaseBuilder(context, DapurJemberDatabase::class.java, DapurJemberDatabase.NAME)
            .allowMainThreadQueries()
            .build()
        repo = BackupRepositoryImpl(context, db, time)
    }

    @After
    fun tearDown() {
        db.close()
        context.getDatabasePath(DapurJemberDatabase.NAME).delete()
        File(context.filesDir, "backups").deleteRecursively()
    }

    @Test
    fun `a backup captures committed data and is listed`() = runTest {
        db.categoryDao().upsert(MenuEntityFixtures.category(id = "c1", name = "Rice"))

        val file = repo.createBackup(passphrase)

        assertTrue(File(file.path).exists())
        assertTrue(file.sizeBytes > 0)
        assertEquals(listOf(file.path), repo.observeBackups().first().map { it.path })
        assertEquals(1_000L, repo.lastBackupAt())
    }

    @Test
    fun `the backup file is encrypted — the data is not readable in the clear`() = runTest {
        db.categoryDao().upsert(MenuEntityFixtures.category(id = "c1", name = "Nasi Goreng Ayam"))

        val file = repo.createBackup(passphrase)

        val raw = String(File(file.path).readBytes(), Charsets.ISO_8859_1)
        assertTrue("backup must start with the DJBK magic", raw.startsWith("DJBK"))
        assertTrue("plaintext leaked into the backup", !raw.contains("Nasi Goreng Ayam"))
    }

    @Test
    fun `restoring an older backup brings back the data it contained`() = runTest {
        db.categoryDao().upsert(MenuEntityFixtures.category(id = "c1", name = "Rice"))
        val backup = repo.createBackup(passphrase)

        // Change the live data after the backup was taken.
        db.categoryDao().upsert(MenuEntityFixtures.category(id = "c2", name = "Drinks"))
        assertEquals(2, db.categoryDao().observeAll().first().size)

        val result = repo.restore(backup, passphrase)
        assertTrue(result is RestoreResult.RestartRequired)

        // The restore closed the database and swapped the file; a fresh handle sees the old state.
        val reopened = Room.databaseBuilder(context, DapurJemberDatabase::class.java, DapurJemberDatabase.NAME)
            .allowMainThreadQueries()
            .build()
        val categories = reopened.categoryDao().observeAll().first()
        reopened.close()
        assertEquals(listOf("Rice"), categories.map { it.name })
    }

    @Test
    fun `restoring with the wrong passphrase fails and leaves the live data untouched`() = runTest {
        db.categoryDao().upsert(MenuEntityFixtures.category(id = "c1", name = "Rice"))
        val backup = repo.createBackup(passphrase)
        db.categoryDao().upsert(MenuEntityFixtures.category(id = "c2", name = "Drinks"))

        val result = repo.restore(backup, "not the passphrase".toCharArray())

        assertTrue(result is RestoreResult.Failed)
        assertEquals("Wrong passphrase, or the backup file is damaged.", (result as RestoreResult.Failed).message)
        assertEquals(2, db.categoryDao().observeAll().first().size) // still both categories
    }

    @Test
    fun `pruning keeps only the newest backups (FR-D3 rolling window)`() = runTest {
        repeat(4) {
            time.now = 1_000L + it * 60_000L
            repo.createBackup(passphrase)
        }
        assertEquals(4, repo.observeBackups().first().size)

        repo.pruneOldBackups(keep = 2)

        val remaining = repo.observeBackups().first()
        assertEquals(2, remaining.size)
        // Newest first, so the two survivors are the most recent pair.
        assertTrue(remaining[0].createdAt >= remaining[1].createdAt)
    }
}
