package com.leanecorps.dapurjember.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.domain.menu.MenuRepository
import com.leanecorps.dapurjember.core.domain.menu.ObserveMenuUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    observeMenu: ObserveMenuUseCase,
    private val menuRepository: MenuRepository,
) : ViewModel() {

    val uiState: StateFlow<MenuUiState> = observeMenu()
        .map { sections -> MenuUiState(sections = sections.map { it.toUi() }, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MenuUiState(),
        )

    /** FR-M2 — toggle sold-out from the list. */
    fun setAvailability(itemId: String, available: Boolean) {
        viewModelScope.launch { menuRepository.setItemAvailability(itemId, available) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
