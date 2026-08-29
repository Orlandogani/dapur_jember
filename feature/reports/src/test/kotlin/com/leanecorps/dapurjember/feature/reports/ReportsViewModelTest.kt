package com.leanecorps.dapurjember.feature.reports

import app.cash.turbine.test
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.reports.DailySummary
import com.leanecorps.dapurjember.core.domain.reports.ItemSales
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeReportsRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeSessionRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeStoreProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ReportsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val session = FakeSessionRepository() // currentBusinessDay() = "2026-08-29"
    private val profiles = FakeStoreProfileRepository()
    private val reports = FakeReportsRepository().apply {
        summaries["2026-08-29"] = DailySummary(
            businessDay = "2026-08-29",
            orderCount = 4,
            covers = 11,
            grossRevenue = Money(400_000),
            paymentMix = emptyList(),
            discountCount = 0,
            discountTotal = Money.ZERO,
            voidedOrders = 0,
            voidedLines = 0,
        )
        itemSales["2026-08-29"] = listOf(ItemSales("Nasi Goreng", 8, Money(200_000)))
        summaries["2026-08-28"] = summaries.getValue("2026-08-29").copy(businessDay = "2026-08-28", orderCount = 1)
    }

    private val viewModel by lazy { ReportsViewModel(session, profiles, reports) }

    @Test
    fun `loads today's summary on open`() = runTest {
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.loading || state.summary == null) state = awaitItem()
            assertEquals("2026-08-29", state.businessDay)
            assertEquals(4, state.summary!!.orderCount)
            assertEquals(listOf("Nasi Goreng"), state.salesByItem.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stepping back a day reloads that day`() = runTest {
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.summary == null) state = awaitItem()

            viewModel.shiftDay(-1)

            var reloaded = awaitItem()
            while (reloaded.businessDay != "2026-08-28" || reloaded.summary == null) reloaded = awaitItem()
            assertEquals(1, reloaded.summary!!.orderCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
