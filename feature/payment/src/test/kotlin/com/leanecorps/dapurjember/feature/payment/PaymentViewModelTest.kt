package com.leanecorps.dapurjember.feature.payment

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.leanecorps.dapurjember.core.domain.order.AddLineParams
import com.leanecorps.dapurjember.core.domain.order.OpenOrderParams
import com.leanecorps.dapurjember.core.domain.order.OrderState
import com.leanecorps.dapurjember.core.domain.order.PaymentMethod
import com.leanecorps.dapurjember.core.domain.order.SettleOrderUseCase
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeOrderRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class PaymentViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val orders = FakeOrderRepository()
    private val session = FakeSessionRepository()

    private suspend fun viewModel(): PaymentViewModel {
        val orderId = orders.openOrder(
            OpenOrderParams(
                orderNumber = "A-1",
                shiftId = "shift-1",
                openedByStaffId = "staff-1",
                businessDay = "2026-08-29",
                diningTableId = "t1",
            ),
        )
        // Fake addLine snapshots a 10_000 unit price.
        orders.addLine(AddLineParams(orderId, menuVariantId = "v1", quantity = 1, addedByStaffId = "staff-1"))
        return PaymentViewModel(
            savedStateHandle = SavedStateHandle(mapOf(PAYMENT_ORDER_ID_ARG to orderId)),
            orderRepository = orders,
            sessionRepository = session,
            settleOrder = SettleOrderUseCase(orders),
        )
    }

    @Test
    fun `a partial payment leaves a balance and does not settle`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(10_000L, expectMostRecentItem().totalMinor)

            vm.pay(PaymentMethod.CASH, amountMinor = 4_000, tenderedMinor = 4_000)

            val state = awaitItem()
            assertEquals(4_000L, state.paidMinor)
            assertEquals(6_000L, state.balanceMinor)
            assertFalse(state.settled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a three-way split paid with three different methods reconciles to the cent`() = runTest {
        // Roadmap M2 exit criterion. 10_000 / 3 does not divide evenly, so this is the case
        // where a naive implementation loses or invents a minor unit.
        val vm = viewModel()
        vm.uiState.test {
            var state = awaitItem()
            while (state.totalMinor == 0L) state = awaitItem()
            assertEquals(10_000L, state.totalMinor)

            vm.startSplit()
            vm.setSplitWays(3)
            var withSplit = expectMostRecentItem()
            while (withSplit.split?.parts?.size != 3) withSplit = awaitItem()

            val parts = withSplit.split!!.parts
            assertEquals(listOf(3_334L, 3_333L, 3_333L), parts.map { it.amountMinor })
            assertEquals(10_000L, withSplit.split!!.partsTotalMinor)

            vm.paySplitPart(0, PaymentMethod.CASH)
            vm.paySplitPart(1, PaymentMethod.CARD)
            vm.paySplitPart(2, PaymentMethod.EWALLET)

            var settled = awaitItem()
            while (!settled.settled) settled = awaitItem()
            assertEquals(0L, settled.balanceMinor)
            assertEquals(10_000L, settled.paidMinor)
            assertEquals(
                listOf(PaymentMethod.CASH, PaymentMethod.CARD, PaymentMethod.EWALLET),
                settled.payments.map { it.method },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a part can only be taken once`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            var state = awaitItem()
            while (state.totalMinor == 0L) state = awaitItem()

            vm.startSplit()
            var withSplit = expectMostRecentItem()
            while (withSplit.split == null) withSplit = awaitItem()

            vm.paySplitPart(0, PaymentMethod.CASH)
            vm.paySplitPart(0, PaymentMethod.CASH) // ignored — already paid

            var latest = expectMostRecentItem()
            while (latest.paidMinor == 0L) latest = awaitItem()
            assertEquals(5_000L, latest.paidMinor) // half of 10_000, not the whole thing twice
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `covering the balance settles the order to PAID`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            expectMostRecentItem()

            vm.pay(PaymentMethod.CASH, amountMinor = 10_000, tenderedMinor = 20_000)

            var settled = awaitItem()
            while (!settled.settled) settled = awaitItem()
            assertEquals(0L, settled.balanceMinor)
            assertEquals(10_000L, settled.payments.single().changeMinor)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(OrderState.PAID, orders.order("order-1")!!.state)
    }
}
