package com.leanecorps.dapurjember.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.common.time.TimeProvider
import com.leanecorps.dapurjember.core.domain.backup.BackupRepository
import com.leanecorps.dapurjember.core.domain.config.StoreProfileRepository
import com.leanecorps.dapurjember.core.domain.order.OpenOrderForTableUseCase
import com.leanecorps.dapurjember.core.domain.shift.ShiftRepository
import com.leanecorps.dapurjember.feature.auth.navigation.PIN_LOCK_ROUTE
import com.leanecorps.dapurjember.feature.settings.navigation.SETUP_ROUTE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppNavHostViewModel @Inject constructor(
    private val openOrderForTable: OpenOrderForTableUseCase,
    private val backupRepository: BackupRepository,
    private val time: TimeProvider,
    shiftRepository: ShiftRepository,
    storeProfileRepository: StoreProfileRepository,
) : ViewModel() {

    private val _backupOverdue = MutableStateFlow(false)

    /** FR-D4: prompt for a backup after [BackupRepository.BACKUP_NAG_DAYS] without one. */
    val backupOverdue: StateFlow<Boolean> = _backupOverdue.asStateFlow()

    init {
        viewModelScope.launch { _backupOverdue.value = backupRepository.backupOverdue(time.nowMillis()) }
    }

    fun dismissBackupReminder() {
        _backupOverdue.value = false
    }

    val hasOpenShift: StateFlow<Boolean> = shiftRepository.observeOpenShift()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    /** null while the DB is still being read; then the route the app should start on (FR-A4). */
    val startRoute: StateFlow<String?> = storeProfileRepository.observeProfile()
        .map { profile -> if (profile == null) SETUP_ROUTE else PIN_LOCK_ROUTE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Resume or open the table's order and return its id (for navigation). */
    suspend fun orderIdForTable(tableId: String, guestCount: Int): String =
        openOrderForTable(tableId, guestCount)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
