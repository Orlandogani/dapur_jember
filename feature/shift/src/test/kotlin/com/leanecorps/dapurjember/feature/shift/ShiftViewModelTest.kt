package com.leanecorps.dapurjember.feature.shift

import app.cash.turbine.test
import com.leanecorps.dapurjember.core.domain.session.Session
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeSessionRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeShiftRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ShiftViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val shift = FakeShiftRepository()
    private val session = FakeSessionRepository(Session("staff-1", "shift-x", "2026-08-29"))
    private fun viewModel() = ShiftViewModel(shift, session)

    @Test
    fun `opening a shift with a float flips shiftOpen`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertFalse(expectMostRecentItem().shiftOpen)
            vm.openShift(500_000)
            val open = expectMostRecentItem()
            assertTrue(open.shiftOpen)
            assertEquals(500_000L, open.openingFloatMinor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the blind close reveals expected vs counted before confirming`() = runTest {
        shift.openShift(500_000, "staff-1", "2026-08-29")
        shift.extraCashMinor = 100_000 // cash sales
        val vm = viewModel()

        vm.uiState.test {
            skipItems(1)
            vm.previewClose(countedCashMinor = 580_000)
            val preview = expectMostRecentItem().closePreview!!
            assertEquals(600_000L, preview.expectedCash.minor)
            assertEquals(580_000L, preview.countedCash.minor)
            assertEquals(-20_000L, preview.variance.minor)

            vm.confirmClose()
            assertTrue(expectMostRecentItem().closed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an unpaid order blocks the close`() = runTest {
        shift.openShift(0, "staff-1", "2026-08-29")
        shift.unpaidOrderIds = listOf("o1", "o2")
        val vm = viewModel()

        vm.uiState.test {
            skipItems(1)
            vm.previewClose(0)
            vm.confirmClose()
            val blocked = expectMostRecentItem()
            assertFalse(blocked.closed)
            assertEquals(2, blocked.blockedByOrders)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
