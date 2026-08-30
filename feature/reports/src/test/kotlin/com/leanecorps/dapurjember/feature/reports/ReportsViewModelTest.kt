package com.leanecorps.dapurjember.feature.reports

import app.cash.turbine.test
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.auth.AuthoriseUseCase
import com.leanecorps.dapurjember.core.domain.auth.StaffRole
import com.leanecorps.dapurjember.core.domain.reports.DailySummary
import com.leanecorps.dapurjember.core.domain.reports.ItemSales
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeAuthRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeReportsRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeSessionRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeStoreProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ReportsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val session = FakeSessionRepository() // currentBusinessDay() = "2026-08-29"
    private val auth = FakeAuthRepository(session).apply {
        addStaff(id = "staff-1", name = "Sari", pin = "1111", role = StaffRole.MANAGER)
    }
    private val profiles = FakeStoreProfileRepository()
    private val reports = FakeReportsRepository().apply {
        summaries["2026-08-29"] = DailySummary(
            businessDay = "2026-08-29",
            orderCount = 4,
            covers = 11,
            grossRevenue = Money(400_000),
            cogs = Money(120_000),
            paymentMix = emptyList(),
            discountCount = 0,
            discountTotal = Money.ZERO,
            voidedOrders = 0,
            voidedLines = 0,
        )
        itemSales["2026-08-29"] = listOf(ItemSales("Nasi Goreng", 8, Money(200_000), Money(60_000)))
        summaries["2026-08-28"] = summaries.getValue("2026-08-29").copy(businessDay = "2026-08-28", orderCount = 1)
    }

    private val viewModel by lazy { ReportsViewModel(session, profiles, reports, AuthoriseUseCase(auth, session)) }

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

    @Test
    fun `a waiter is refused and the figures are never loaded`() = runTest {
        val waiterAuth = FakeAuthRepository(session).apply {
            addStaff(id = "staff-1", name = "Wira", pin = "1111", role = StaffRole.WAITER)
        }
        val vm = ReportsViewModel(session, profiles, reports, AuthoriseUseCase(waiterAuth, session))

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertNull(state.summary)
            assertEquals("", state.businessDay)
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(vm.canView.value)
    }
}
