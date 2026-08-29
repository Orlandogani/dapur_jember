package com.leanecorps.dapurjember.feature.order

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.MenuVariant
import com.leanecorps.dapurjember.core.domain.menu.Modifier
import com.leanecorps.dapurjember.core.domain.menu.ModifierGroup
import com.leanecorps.dapurjember.core.domain.menu.ObserveMenuUseCase
import com.leanecorps.dapurjember.core.domain.order.OpenOrderParams
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeMenuRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeOrderRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class OrderViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val menu = FakeMenuRepository()
    private val orders = FakeOrderRepository()
    private val session = FakeSessionRepository()

    private suspend fun viewModel(): OrderViewModel {
        menu.upsertCategory(Category(id = "c1", name = "Rice"))
        menu.upsertItem(MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng"))
        menu.upsertVariant(MenuVariant(id = "v1", menuItemId = "i1", name = "Regular", price = Money(15_000)))
        val orderId = orders.openOrder(
            OpenOrderParams(
                orderNumber = "A-1",
                shiftId = "shift-1",
                openedByStaffId = "staff-1",
                businessDay = "2026-08-29",
                diningTableId = "t1",
            ),
        )
        return OrderViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ORDER_ID_ARG to orderId)),
            observeMenu = ObserveMenuUseCase(menu),
            menuRepository = menu,
            orderRepository = orders,
            sessionRepository = session,
        )
    }

    @Test
    fun `the first category is auto-selected and its tiles are shown`() = runTest {
        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals("A-1", state.orderNumber)
            assertEquals("c1", state.selectedCategoryId)
            assertEquals(listOf("Nasi Goreng"), state.board.map { it.name })
            assertEquals(15_000L, state.board.single().priceMinor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tapping a tile adds a line and enables Send`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            val initial = expectMostRecentItem()
            assertTrue(initial.lines.isEmpty())

            vm.addTile(initial.board.single())

            val withLine = awaitItem()
            assertEquals(1, withLine.lines.size)
            assertEquals(1, withLine.lines.single().quantity)
            assertTrue(withLine.canSend)
            assertTrue(withLine.totals.totalMinor > 0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tapping an item with a required modifier group opens the picker and blocks add until satisfied`() = runTest {
        menu.upsertCategory(Category(id = "c1", name = "Rice"))
        menu.upsertItem(MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng"))
        menu.upsertVariant(MenuVariant(id = "v1", menuItemId = "i1", name = "Regular", price = Money(15_000)))
        menu.saveModifierGroup(
            ModifierGroup(id = "g1", name = "Spice", minSelect = 1, maxSelect = 1, required = true),
            listOf(
                Modifier(id = "m1", modifierGroupId = "g1", name = "Mild"),
                Modifier(id = "m2", modifierGroupId = "g1", name = "Hot", priceDelta = Money(2_000)),
            ),
        )
        menu.setItemModifierGroups("i1", listOf("g1"))
        val orderId = orders.openOrder(
            OpenOrderParams(
                orderNumber = "A-1",
                shiftId = "s",
                openedByStaffId = "staff-1",
                businessDay = "2026-08-29",
                diningTableId = "t1",
            ),
        )
        val vm = OrderViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ORDER_ID_ARG to orderId)),
            observeMenu = ObserveMenuUseCase(menu),
            menuRepository = menu,
            orderRepository = orders,
            sessionRepository = session,
        )

        vm.uiState.test {
            vm.addTile(expectMostRecentItem().board.single())

            var withPicker = awaitItem()
            while (withPicker.picker == null) withPicker = awaitItem()
            assertEquals(1, withPicker.picker!!.groups.size)
            assertTrue(!withPicker.picker!!.canConfirm) // nothing chosen for a required group
            assertTrue(withPicker.lines.isEmpty())

            vm.toggleModifier("g1", "m2")
            assertTrue(awaitItem().picker!!.canConfirm)

            vm.confirmPicker()
            var done = awaitItem()
            while (done.picker != null) done = awaitItem()
            assertEquals(1, done.lines.size)
            assertTrue(done.lines.single().name.contains("m2")) // fake stores modifierId as the name
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `send marks lines sent and clears canSend`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            vm.addTile(expectMostRecentItem().board.single())
            assertTrue(awaitItem().canSend)

            vm.send()

            val sent = awaitItem()
            assertTrue(sent.lines.single().sent)
            assertTrue(!sent.canSend)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
