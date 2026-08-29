package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.reports.DailySummary
import com.leanecorps.dapurjember.core.domain.reports.ItemSales
import com.leanecorps.dapurjember.core.domain.reports.ReportsRepository

/** Returns whatever was put in [summaries] / [itemSales] per business day. */
class FakeReportsRepository : ReportsRepository {

    val summaries = mutableMapOf<String, DailySummary>()
    val itemSales = mutableMapOf<String, List<ItemSales>>()

    override suspend fun dailySummary(businessDay: String): DailySummary =
        summaries[businessDay] ?: DailySummary(
            businessDay = businessDay,
            orderCount = 0,
            covers = 0,
            grossRevenue = Money.ZERO,
            paymentMix = emptyList(),
            discountCount = 0,
            discountTotal = Money.ZERO,
            voidedOrders = 0,
            voidedLines = 0,
        )

    override suspend fun salesByItem(businessDay: String): List<ItemSales> = itemSales[businessDay].orEmpty()
}
