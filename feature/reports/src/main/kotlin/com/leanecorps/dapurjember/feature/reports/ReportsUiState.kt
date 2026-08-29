package com.leanecorps.dapurjember.feature.reports

import com.leanecorps.dapurjember.core.domain.reports.DailySummary
import com.leanecorps.dapurjember.core.domain.reports.ItemSales

data class ReportsUiState(
    val loading: Boolean = true,
    val businessDay: String = "",
    val currencyCode: String = "",
    val currencyMinorUnits: Int = 0,
    val summary: DailySummary? = null,
    val salesByItem: List<ItemSales> = emptyList(),
)
