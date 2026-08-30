package com.leanecorps.dapurjember.feature.settings.backup

import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeBackupRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val backups = FakeBackupRepository()
    private val viewModel by lazy { BackupViewModel(backups) }

    /**
     * `uiState` is `stateIn(WhileSubscribed)`, so it only tracks the sources while something
     * collects it. Keep a collector alive for the duration of each test.
     */
    private fun TestScope.subscribe() {
        backgroundScope.launch { viewModel.uiState.collect { } }
    }

    @Test
    fun `a backup needs a long enough passphrase, entered twice`() = runTest {
        subscribe()
        viewModel.startCreate()
        runCurrent()
        viewModel.editCreate { it.copy(passphrase = "short") }
        assertTrue(viewModel.uiState.value.createDialog!!.tooShort)
        assertFalse(viewModel.uiState.value.createDialog!!.canCreate)

        viewModel.editCreate { it.copy(passphrase = "hunter2hunter2", confirm = "typo") }
        assertTrue(viewModel.uiState.value.createDialog!!.mismatch)
        assertFalse(viewModel.uiState.value.createDialog!!.canCreate)

        viewModel.editCreate { it.copy(confirm = "hunter2hunter2") }
        assertTrue(viewModel.uiState.value.createDialog!!.canCreate)

        viewModel.confirmCreate()
        advanceUntilIdle()
        assertEquals("hunter2hunter2", backups.lastCreatedWith)
        assertEquals(1, backups.observeBackups().first().size)
    }

    @Test
    fun `restore requires both the passphrase and an explicit acknowledgement`() = runTest {
        subscribe()
        val file = backups.createBackup("hunter2hunter2".toCharArray())
        viewModel.startRestore(file)
        runCurrent()

        viewModel.editRestore { it.copy(passphrase = "hunter2hunter2") }
        assertFalse(viewModel.uiState.value.restoreDialog!!.canRestore) // not acknowledged

        viewModel.editRestore { it.copy(acknowledged = true) }
        assertTrue(viewModel.uiState.value.restoreDialog!!.canRestore)
    }

    @Test
    fun `a wrong passphrase reports the failure and does not claim a restart is needed`() = runTest {
        subscribe()
        val file = backups.createBackup("hunter2hunter2".toCharArray())
        viewModel.startRestore(file)
        runCurrent()
        viewModel.editRestore { it.copy(passphrase = "wrong one", acknowledged = true) }

        viewModel.confirmRestore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.restartRequired)
        assertEquals("Wrong passphrase, or the backup file is damaged.", state.message)
        assertEquals("", state.restoreDialog!!.passphrase) // cleared, dialog stays open to retry
    }

    @Test
    fun `a successful restore asks for a restart`() = runTest {
        subscribe()
        val file = backups.createBackup("hunter2hunter2".toCharArray())
        viewModel.startRestore(file)
        runCurrent()
        viewModel.editRestore { it.copy(passphrase = "hunter2hunter2", acknowledged = true) }

        viewModel.confirmRestore()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.restartRequired)
    }
}
