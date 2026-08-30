package com.leanecorps.dapurjember.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.domain.config.StoreProfileRepository
import com.leanecorps.dapurjember.core.domain.reports.ReportCsv
import com.leanecorps.dapurjember.core.domain.reports.ReportsRepository
import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _csvExports = Channel<CsvExport>(Channel.BUFFERED)

    /** One-shot CSV exports; the screen collects these and opens the share sheet (arch §2). */
    val csvExports: Flow<CsvExport> = _csvExports.receiveAsFlow()

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
                    salesByCategory = reports.salesByCategory(day),
                    audit = reports.auditEntries(day),
                ),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ReportsUiState())

    fun exportCsv() {
        val state = uiState.value
        val summary = state.summary ?: return
        viewModelScope.launch {
            _csvExports.send(
                CsvExport(
                    fileName = "dapurjember-${state.businessDay}.csv",
                    content = ReportCsv.dailySummary(
                        summary = summary,
                        items = state.salesByItem,
                        categories = state.salesByCategory,
                        audit = state.audit,
                        currencyMinorUnits = state.currencyMinorUnits,
                    ),
                ),
            )
        }
    }

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
