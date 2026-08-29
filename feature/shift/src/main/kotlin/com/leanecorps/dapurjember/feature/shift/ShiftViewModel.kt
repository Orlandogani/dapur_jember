package com.leanecorps.dapurjember.feature.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import com.leanecorps.dapurjember.core.domain.shift.OpenOrdersBlockCloseException
import com.leanecorps.dapurjember.core.domain.shift.ShiftCloseResult
import com.leanecorps.dapurjember.core.domain.shift.ShiftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShiftUiState(
    val loading: Boolean = true,
    val shiftOpen: Boolean = false,
    val openingFloatMinor: Long = 0,
    /** Set after a blind count is submitted (FR-S4) — reveals expected vs counted. */
    val closePreview: ShiftCloseResult? = null,
    val blockedByOrders: Int = 0,
    val closed: Boolean = false,
)

@HiltViewModel
class ShiftViewModel @Inject constructor(
    private val shiftRepository: ShiftRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private data class LocalState(
        val preview: ShiftCloseResult? = null,
        val blockedByOrders: Int = 0,
        val closed: Boolean = false,
    )

    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<ShiftUiState> =
        combine(shiftRepository.observeOpenShift(), local) { shift, state ->
            ShiftUiState(
                loading = false,
                shiftOpen = shift != null,
                openingFloatMinor = shift?.openingFloat?.minor ?: 0,
                closePreview = state.preview,
                blockedByOrders = state.blockedByOrders,
                closed = state.closed,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ShiftUiState())

    fun openShift(openingFloatMinor: Long) {
        viewModelScope.launch {
            val staffId = sessionRepository.currentStaffId() ?: return@launch
            shiftRepository.openShift(openingFloatMinor, staffId, sessionRepository.currentBusinessDay())
        }
    }

    /** FR-S4: preview expected vs counted for the blind Z-report. */
    fun previewClose(countedCashMinor: Long) {
        viewModelScope.launch {
            val shift = shiftRepository.observeOpenShift().first() ?: return@launch
            val expected = shiftRepository.expectedCashMinor(shift.id)
            local.value = local.value.copy(
                preview = ShiftCloseResult(
                    expectedCash = Money(expected),
                    countedCash = Money(countedCashMinor),
                    variance = Money(countedCashMinor - expected),
                ),
            )
        }
    }

    fun confirmClose() {
        val preview = local.value.preview ?: return
        viewModelScope.launch {
            val staffId = sessionRepository.currentStaffId() ?: return@launch
            val shift = shiftRepository.observeOpenShift().first() ?: return@launch
            runCatching { shiftRepository.closeShift(shift.id, preview.countedCash.minor, staffId) }
                .onSuccess { local.value = LocalState(closed = true) }
                .onFailure { error ->
                    if (error is OpenOrdersBlockCloseException) {
                        local.value = local.value.copy(blockedByOrders = error.orderIds.size, preview = null)
                    }
                }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
