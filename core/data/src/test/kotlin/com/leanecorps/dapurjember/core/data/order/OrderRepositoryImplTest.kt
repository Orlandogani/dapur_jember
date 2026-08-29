package com.leanecorps.dapurjember.core.data.order

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.leanecorps.dapurjember.core.data.database.AuditLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.order.AddLineParams
import com.leanecorps.dapurjember.core.domain.order.ApplyDiscountParams
import com.leanecorps.dapurjember.core.domain.order.DiscountKind
import com.leanecorps.dapurjember.core.domain.order.IllegalOrderTransitionException
import com.leanecorps.dapurjember.core.domain.order.OpenOrderParams
import com.leanecorps.dapurjember.core.domain.order.OrderEvent
import com.leanecorps.dapurjember.core.domain.order.OrderState
import com.leanecorps.dapurjember.core.domain.order.PaymentMethod
import com.leanecorps.dapurjember.core.domain.order.RecordPaymentParams
import com.leanecorps.dapurjember.core.domain.printing.TicketPrinter
import com.leanecorps.dapurjember.core.testing.FakeTimeProvider
import com.leanecorps.dapurjember.core.testing.database.MenuEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.seedOrderPrerequisites
import com.leanecorps.dapurjember.core.testing.repository.FakePrintQueue
import com.leanecorps.dapurjember.core.testing.repository.FakeTicketRenderer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrderRepositoryImplTest {

    private lateinit var db: DapurJemberDatabase
    private lateinit var repo: OrderRepositoryImpl
    private val time = FakeTimeProvider(now = 1_000L)
    private val printQueue = FakePrintQueue()
    private val ticketRenderer = FakeTicketRenderer()

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DapurJemberDatabase::class.java)
            .allowMainThreadQueries().build()
        db.seedOrderPrerequisites()
        val deviceIds = DeviceIdProvider(context)
        repo = OrderRepositoryImpl(
            db = db,
            orderDao = db.orderDao(),
            lineDao = db.orderLineDao(),
            lineModifierDao = db.orderLineModifierDao(),
            paymentDao = db.paymentDao(),
            discountDao = db.discountDao(),
            menuVariantDao = db.menuVariantDao(),
            menuItemDao = db.menuItemDao(),
            modifierDao = db.modifierDao(),
            storeProfileDao = db.storeProfileDao(),
            changeLog = ChangeLogRecorder(db.changeLogDao(), deviceIds),
            auditLog = AuditLogRecorder(db.auditLogDao()),
            time = time,
            deviceIds = deviceIds,
            ticketAssembler = TicketAssembler(
                orderDao = db.orderDao(),
                lineDao = db.orderLineDao(),
                lineModifierDao = db.orderLineModifierDao(),
                paymentDao = db.paymentDao(),
                staffDao = db.staffDao(),
                tableDao = db.diningTableDao(),
                storeProfileDao = db.storeProfileDao(),
                printerConfigDao = db.printerConfigDao(),
            ),
            ticketPrinter = TicketPrinter(ticketRenderer, printQueue),
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun openOrder() = repo.openOrder(
        OpenOrderParams(
            orderNumber = "A-001",
            shiftId = "shift-1",
            openedByStaffId = "staff-1",
            businessDay = "2026-08-29",
            diningTableId = "table-1",
            guestCount = 2,
        ),
    )

    private suspend fun addLine(orderId: String, qty: Int = 1) = repo.addLine(
        AddLineParams(
            orderId = orderId,
            menuVariantId = "var-1",
            quantity = qty,
            addedByStaffId = "staff-1",
        ),
    )

    @Test
    fun `openOrder creates a DRAFT order and logs it`() = runTest {
        val id = openOrder()
        val order = repo.getOrder(id)!!

        assertEquals(OrderState.DRAFT, order.state)
        assertEquals("A-001", order.orderNumber)
        val log = db.changeLogDao().observeUnsynced().first().single()
        assertEquals("orders" to "INSERT", log.entityType to log.op)
    }

    @Test
    fun `addLine snapshots the menu and recomputes the subtotal`() = runTest {
        val id = openOrder()
        addLine(id, qty = 2)

        val order = repo.observeOrder(id).first()!!
        assertEquals(1, order.lines.size)
        assertEquals("Nasi Goreng Ayam", order.lines.single().itemName)
        assertEquals(15_000L, order.lines.single().unitPrice.minor)
        assertEquals(30_000L, order.totals.subtotal.minor)
        assertEquals(30_000L, order.totals.total.minor)
    }

    @Test
    fun `totals pick up the store profile's tax rule`() = runTest {
        db.storeProfileDao().upsert(MenuEntityFixtures.storeProfile().copy(taxRateBp = 1_000, taxInclusive = false))
        val id = openOrder()
        addLine(id, qty = 2)

        val order = repo.getOrder(id)!!
        assertEquals(30_000L, order.totals.subtotal.minor)
        assertEquals(3_000L, order.totals.tax.minor)
        assertEquals(33_000L, order.totals.total.minor)
    }

    @Test
    fun `sendToKitchen prints only the unsent lines and advances DRAFT to SENT`() = runTest {
        val id = openOrder()
        addLine(id)
        time.advanceBy(1)

        val firstBatch = repo.sendToKitchen(id)
        assertEquals(1, firstBatch.size)
        assertEquals(OrderState.SENT, repo.getOrder(id)!!.state)

        time.advanceBy(1)
        addLine(id)
        time.advanceBy(1)
        val secondBatch = repo.sendToKitchen(id)
        assertEquals(1, secondBatch.size)

        assertTrue(repo.sendToKitchen(id).isEmpty())
    }

    @Test
    fun `sendToKitchen queues a kitchen ticket carrying the sent lines`() = runTest {
        val id = openOrder()
        addLine(id)
        time.advanceBy(1)

        repo.sendToKitchen(id)

        val job = printQueue.enqueued.single()
        assertEquals("KITCHEN", job.type.name)
        assertEquals("A-001", ticketRenderer.kitchenTickets.single().orderNumber)
        assertEquals(1, ticketRenderer.kitchenTickets.single().lines.size)
    }

    @Test
    fun `a re-send with no new lines queues nothing`() = runTest {
        val id = openOrder()
        addLine(id)
        time.advanceBy(1)
        repo.sendToKitchen(id)
        repo.sendToKitchen(id)

        assertEquals(1, printQueue.enqueued.size)
    }

    @Test
    fun `reaching PAID queues a customer receipt exactly once`() = runTest {
        db.storeProfileDao().upsert(MenuEntityFixtures.storeProfile().copy(taxRateBp = 0))
        val id = openOrder()
        addLine(id)
        time.advanceBy(1)
        repo.sendToKitchen(id)
        repo.applyEvent(id, OrderEvent.SERVE)

        repo.applyEvent(id, OrderEvent.PAY)

        assertEquals(1, printQueue.enqueued.count { it.type.name == "RECEIPT" })
        assertEquals("A-001", ticketRenderer.receipts.single().orderNumber)
    }

    @Test
    fun `voidLine writes an audit_log row and drops the line from the total`() = runTest {
        db.storeProfileDao().upsert(MenuEntityFixtures.storeProfile().copy(taxRateBp = 0))
        val id = openOrder()
        val lineId = addLine(id, qty = 2)
        time.advanceBy(1)

        repo.voidLine(lineId, reason = "wrong order", actorStaffId = "staff-1")

        assertEquals(0L, repo.getOrder(id)!!.totals.subtotal.minor)
        assertTrue(repo.getOrder(id)!!.lines.single().voided)
        val audit = db.auditLogDao().observeRecent(10).first().single()
        assertEquals("VOID_LINE", audit.action)
        assertEquals(lineId, audit.entityId)
    }

    @Test
    fun `applyEvent rejects an illegal transition`() = runTest {
        val id = openOrder()
        val failure = runCatching { repo.applyEvent(id, OrderEvent.PAY) }.exceptionOrNull()

        assertTrue("expected an illegal-transition error, got $failure", failure is IllegalOrderTransitionException)
        assertEquals(OrderState.DRAFT, repo.getOrder(id)!!.state)
    }

    @Test
    fun `getOrder returns null for an unknown id`() = runTest {
        assertNull(repo.getOrder("nope"))
    }

    @Test
    fun `a whole-bill percentage discount reduces the total and is audited`() = runTest {
        db.storeProfileDao().upsert(MenuEntityFixtures.storeProfile().copy(taxRateBp = 0))
        val id = openOrder()
        addLine(id, qty = 2) // 30_000
        time.advanceBy(1)

        repo.applyDiscount(
            ApplyDiscountParams(
                orderId = id,
                kind = DiscountKind.PERCENT,
                value = 1_000,
                reason = "loyalty",
                authorisedByStaffId = "staff-1",
            ),
        )

        val order = repo.getOrder(id)!!
        assertEquals(3_000L, order.totals.discount.minor)
        assertEquals(27_000L, order.totals.total.minor)
        assertEquals(1, order.discounts.size)
        assertEquals("APPLY_DISCOUNT", db.auditLogDao().observeRecent(10).first().single().action)
    }

    @Test
    fun `recordPayment tracks the balance and isFullySettled`() = runTest {
        db.storeProfileDao().upsert(MenuEntityFixtures.storeProfile().copy(taxRateBp = 0))
        val id = openOrder()
        addLine(id, qty = 2) // total 30_000

        assertEquals(false, repo.isFullySettled(id))

        repo.recordPayment(
            RecordPaymentParams(
                orderId = id,
                method = PaymentMethod.CASH,
                amountMinor = 20_000,
                tenderedMinor = 20_000,
                staffId = "staff-1",
            ),
        )
        assertEquals(10_000L, repo.getOrder(id)!!.balanceDue.minor)
        assertEquals(false, repo.isFullySettled(id))

        repo.recordPayment(
            RecordPaymentParams(
                orderId = id,
                method = PaymentMethod.EWALLET,
                amountMinor = 10_000,
                tenderedMinor = 10_000,
                staffId = "staff-1",
            ),
        )
        assertEquals(0L, repo.getOrder(id)!!.balanceDue.minor)
        assertEquals(true, repo.isFullySettled(id))
    }

    @Test
    fun `cash change is recorded`() = runTest {
        db.storeProfileDao().upsert(MenuEntityFixtures.storeProfile().copy(taxRateBp = 0))
        val id = openOrder()
        addLine(id) // 15_000

        repo.recordPayment(
            RecordPaymentParams(
                orderId = id,
                method = PaymentMethod.CASH,
                amountMinor = 15_000,
                tenderedMinor = 20_000,
                staffId = "staff-1",
            ),
        )
        assertEquals(5_000L, repo.getOrder(id)!!.payments.single().change.minor)
    }
}
