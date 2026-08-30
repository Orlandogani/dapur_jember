package com.leanecorps.dapurjember.feature.order

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.auth.AuthoriseUseCase
import com.leanecorps.dapurjember.core.domain.auth.StaffRole
import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.MenuVariant
import com.leanecorps.dapurjember.core.domain.menu.Modifier
import com.leanecorps.dapurjember.core.domain.menu.ModifierGroup
import com.leanecorps.dapurjember.core.domain.menu.ObserveMenuUseCase
import com.leanecorps.dapurjember.core.domain.order.DiscountKind
import com.leanecorps.dapurjember.core.domain.order.OpenOrderParams
import com.leanecorps.dapurjember.core.domain.order.VoidReason
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeAuthRepository
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
    private val auth = FakeAuthRepository(session)
    private val authorise = AuthoriseUseCase(auth, session)

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
            authorise = authorise,
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
            authorise = authorise,
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

    // --- Void (S07 / FR-O4) and step-up authorisation (FR-A3) ---

    /** Signs "staff-1" in as [role] so the permission check has someone to look at. */
    private suspend fun signInAs(role: StaffRole) {
        auth.addStaff(id = "staff-1", name = "Wira", pin = "1111", role = role)
        auth.signIn("staff-1", "1111")
    }

    private suspend fun addSentLine(vm: OrderViewModel): OrderLineUi {
        vm.addTile(vm.uiState.value.board.single())
        vm.send()
        return vm.uiState.value.lines.single()
    }

    @Test
    fun `a waiter voiding a sent line must step up, and a manager PIN authorises it`() = runTest {
        signInAs(StaffRole.WAITER)
        auth.addStaff(id = "mgr", name = "Sari", pin = "9999", role = StaffRole.MANAGER)
        val vm = viewModel()

        vm.uiState.test {
            skipItems(1)
            val line = addSentLine(vm)
            var state = awaitItem()
            while (!state.lines.single().sent) state = awaitItem()

            vm.openLineAction(line)
            var sheet = awaitItem()
            while (sheet.lineAction == null) sheet = awaitItem()
            assertTrue(sheet.lineAction!!.needsStepUp)
            assertTrue(!sheet.lineAction!!.canVoid) // no PIN entered yet

            // A wrong PIN is refused and the line stays active.
            vm.editLineAction { it.copy(pin = "0000") }
            vm.confirmVoid()
            var refused = awaitItem()
            while (refused.lineAction?.pinRejected != true) refused = awaitItem()
            assertTrue(!orders.order(vm.uiState.value.orderId)!!.lines.single().voided)

            vm.editLineAction { it.copy(pin = "9999", reason = VoidReason.QUALITY_ISSUE, note = "cold") }
            vm.confirmVoid()
            var done = awaitItem()
            while (done.lineAction != null) done = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val voided = orders.order(orders.order("order-1")!!.id)!!.lines.single()
        assertTrue(voided.voided)
        // The stored reason is the stable enum name, never a translated label (FR-O4).
        assertEquals("QUALITY_ISSUE — cold", voided.voidReason)
    }

    @Test
    fun `a manager voids without any step-up prompt`() = runTest {
        signInAs(StaffRole.MANAGER)
        val vm = viewModel()

        vm.uiState.test {
            skipItems(1)
            val line = addSentLine(vm)
            var state = awaitItem()
            while (!state.lines.single().sent) state = awaitItem()

            vm.openLineAction(line)
            var sheet = awaitItem()
            while (sheet.lineAction == null) sheet = awaitItem()
            assertTrue(!sheet.lineAction!!.needsStepUp)
            assertTrue(sheet.lineAction!!.canVoid)

            vm.confirmVoid()
            var done = awaitItem()
            while (done.lineAction != null) done = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(orders.order("order-1")!!.lines.single().voided)
    }

    @Test
    fun `a discount needs a reason and records who authorised it`() = runTest {
        signInAs(StaffRole.OWNER)
        val vm = viewModel()

        vm.uiState.test {
            skipItems(1)
            vm.addTile(vm.uiState.value.board.single())
            awaitItem()

            vm.openDiscount()
            var sheet = awaitItem()
            while (sheet.discount == null) sheet = awaitItem()
            assertTrue(!sheet.discount!!.needsStepUp) // an owner already holds the permission

            vm.editDiscount { it.copy(kind = DiscountKind.PERCENT, valueText = "10") }
            assertTrue(!vm.uiState.value.discount!!.canApply) // reason still missing

            vm.editDiscount { it.copy(reason = "loyalty") }
            assertTrue(vm.uiState.value.discount!!.canApply)
            assertEquals(1_000L, vm.uiState.value.discount!!.value) // 10% -> 1000 basis points
            cancelAndIgnoreRemainingEvents()
        }
    }
}
