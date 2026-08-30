package com.leanecorps.dapurjember.core.data.reports

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.data.database.dao.ReportsDao
import com.leanecorps.dapurjember.core.domain.order.PaymentMethod
import com.leanecorps.dapurjember.core.domain.reports.DailySummary
import com.leanecorps.dapurjember.core.domain.reports.ItemSales
import com.leanecorps.dapurjember.core.domain.reports.PaymentMixRow
import com.leanecorps.dapurjember.core.domain.reports.ReportsRepository
import javax.inject.Inject

internal class ReportsRepositoryImpl @Inject constructor(
    private val dao: ReportsDao,
) : ReportsRepository {

    override suspend fun dailySummary(businessDay: String): DailySummary {
        val totals = dao.dailyTotals(businessDay)
        val discounts = dao.discounts(businessDay)
        return DailySummary(
            businessDay = businessDay,
            orderCount = totals.orderCount,
            covers = totals.covers,
            grossRevenue = Money(totals.revenueMinor),
            cogs = Money(dao.cogsMinor(businessDay)),
            paymentMix = dao.paymentMix(businessDay).map {
                PaymentMixRow(
                    method = runCatching { PaymentMethod.valueOf(it.method) }.getOrDefault(PaymentMethod.OTHER),
                    amount = Money(it.amountMinor),
                )
            },
            discountCount = discounts.count,
            discountTotal = Money(discounts.totalMinor),
            voidedOrders = dao.voidedOrderCount(businessDay),
            voidedLines = dao.voidedLineCount(businessDay),
        )
    }

    override suspend fun salesByItem(businessDay: String): List<ItemSales> =
        dao.salesByItem(businessDay).map {
            ItemSales(name = it.name, quantity = it.quantity, gross = Money(it.grossMinor), cost = Money(it.costMinor))
        }
}
