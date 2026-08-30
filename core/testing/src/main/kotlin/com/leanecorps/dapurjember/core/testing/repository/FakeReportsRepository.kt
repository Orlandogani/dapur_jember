package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.reports.AuditEntry
import com.leanecorps.dapurjember.core.domain.reports.CategorySales
import com.leanecorps.dapurjember.core.domain.reports.DailySummary
import com.leanecorps.dapurjember.core.domain.reports.ItemSales
import com.leanecorps.dapurjember.core.domain.reports.ReportsRepository

/** Returns whatever was put in [summaries] / [itemSales] per business day. */
class FakeReportsRepository : ReportsRepository {

    val summaries = mutableMapOf<String, DailySummary>()
    val itemSales = mutableMapOf<String, List<ItemSales>>()
    val categorySales = mutableMapOf<String, List<CategorySales>>()
    val audit = mutableMapOf<String, List<AuditEntry>>()

    override suspend fun dailySummary(businessDay: String): DailySummary =
        summaries[businessDay] ?: DailySummary(
            businessDay = businessDay,
            orderCount = 0,
            covers = 0,
            grossRevenue = Money.ZERO,
            cogs = Money.ZERO,
            paymentMix = emptyList(),
            discountCount = 0,
            discountTotal = Money.ZERO,
            voidedOrders = 0,
            voidedLines = 0,
        )

    override suspend fun salesByItem(businessDay: String): List<ItemSales> = itemSales[businessDay].orEmpty()

    override suspend fun salesByCategory(businessDay: String): List<CategorySales> =
        categorySales[businessDay].orEmpty()

    override suspend fun auditEntries(businessDay: String): List<AuditEntry> = audit[businessDay].orEmpty()
}
