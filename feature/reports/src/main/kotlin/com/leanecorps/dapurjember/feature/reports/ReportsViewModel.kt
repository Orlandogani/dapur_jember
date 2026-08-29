package com.leanecorps.dapurjember.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.domain.config.StoreProfileRepository
import com.leanecorps.dapurjember.core.domain.reports.ReportsRepository
import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    session: SessionRepository,
    private val storeProfiles: StoreProfileRepository,
    private val reports: ReportsRepository,
) : ViewModel() {

    private val businessDay = MutableStateFlow<String?>(null)
    private var currencyCode = ""
    private var currencyMinorUnits = 0

    val uiState: StateFlow<ReportsUiState> = businessDay.filterNotNull().flatMapLatest { day ->
        flow {
            emit(ReportsUiState(loading = true, businessDay = day))
            emit(
                ReportsUiState(
                    loading = false,
                    businessDay = day,
                    currencyCode = currencyCode,
                    currencyMinorUnits = currencyMinorUnits,
                    summary = reports.dailySummary(day),
                    salesByItem = reports.salesByItem(day),
                ),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ReportsUiState())

    init {
        viewModelScope.launch {
            storeProfiles.getProfile()?.let {
                currencyCode = it.currencyCode
                currencyMinorUnits = it.currencyMinorUnits
            }
            businessDay.value = session.currentBusinessDay()
        }
    }

    fun setBusinessDay(value: String) {
        businessDay.value = value.trim()
    }

    fun shiftDay(deltaDays: Long) {
        val current = businessDay.value ?: return
        runCatching { LocalDate.parse(current).plusDays(deltaDays).toString() }
            .onSuccess { businessDay.value = it }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
