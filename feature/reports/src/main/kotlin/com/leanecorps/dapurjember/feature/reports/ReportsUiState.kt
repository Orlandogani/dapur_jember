package com.leanecorps.dapurjember.feature.reports

import com.leanecorps.dapurjember.core.domain.reports.AuditEntry
import com.leanecorps.dapurjember.core.domain.reports.CategorySales
import com.leanecorps.dapurjember.core.domain.reports.DailySummary
import com.leanecorps.dapurjember.core.domain.reports.ItemSales

data class ReportsUiState(
    val loading: Boolean = true,
    val businessDay: String = "",
    val currencyCode: String = "",
    val currencyMinorUnits: Int = 0,
    val summary: DailySummary? = null,
    val salesByItem: List<ItemSales> = emptyList(),
    val salesByCategory: List<CategorySales> = emptyList(),
    val audit: List<AuditEntry> = emptyList(),
)

/** One-shot: the rendered CSV to hand to the Android share sheet (FR-R3). */
data class CsvExport(val fileName: String, val content: String)
