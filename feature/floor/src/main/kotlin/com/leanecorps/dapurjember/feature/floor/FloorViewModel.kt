package com.leanecorps.dapurjember.feature.floor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.domain.floor.FloorRepository
import com.leanecorps.dapurjember.core.domain.floor.TableState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FloorViewModel @Inject constructor(
    private val floorRepository: FloorRepository,
) : ViewModel() {

    val uiState: StateFlow<FloorUiState> = floorRepository.observeFloor()
        .map { areas -> FloorUiState(areas = areas.map { it.toUi() }, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = FloorUiState(),
        )

    fun markTableState(tableId: String, state: TableState) {
        viewModelScope.launch { floorRepository.setTableState(tableId, state) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
