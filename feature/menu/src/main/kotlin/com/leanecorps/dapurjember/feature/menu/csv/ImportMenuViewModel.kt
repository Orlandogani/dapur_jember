package com.leanecorps.dapurjember.feature.menu.csv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.domain.auth.AuthoriseUseCase
import com.leanecorps.dapurjember.core.domain.auth.Permission
import com.leanecorps.dapurjember.core.domain.menu.ImportMenuCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SAMPLE = "category,item,variant,price\n" +
    "Rice,Nasi Goreng Ayam,Regular,25000\n" +
    "Rice,Nasi Goreng Ayam,Large,30000\n" +
    "Drinks,Es Teh,,5000"

data class ImportMenuUiState(
    val text: String = SAMPLE,
    val running: Boolean = false,
    val summary: ImportMenuCsvUseCase.Summary? = null,
    /** False when the signed-in staff lacks MANAGE_MENU; importing is blocked. */
    val canManage: Boolean = false,
)

@HiltViewModel
class ImportMenuViewModel @Inject constructor(
    private val importMenuCsv: ImportMenuCsvUseCase,
    private val authorise: AuthoriseUseCase,
) : ViewModel() {

    private val state = MutableStateFlow(ImportMenuUiState())
    val uiState: StateFlow<ImportMenuUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val allowed = authorise.currentUserCan(Permission.MANAGE_MENU)
            state.update { it.copy(canManage = allowed) }
        }
    }

    fun setText(value: String) = state.update { it.copy(text = value, summary = null) }

    fun import() {
        if (state.value.running || state.value.text.isBlank() || !state.value.canManage) return
        state.update { it.copy(running = true, summary = null) }
        viewModelScope.launch {
            val summary = importMenuCsv(state.value.text)
            state.update { it.copy(running = false, summary = summary) }
        }
    }
}
