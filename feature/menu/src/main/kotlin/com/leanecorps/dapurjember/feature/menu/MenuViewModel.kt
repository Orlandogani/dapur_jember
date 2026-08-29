package com.leanecorps.dapurjember.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.domain.menu.Category
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

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val nextOrder = uiState.value.sections.size
            menuRepository.upsertCategory(Category(id = UuidV7.generate(), name = trimmed, sortOrder = nextOrder))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
