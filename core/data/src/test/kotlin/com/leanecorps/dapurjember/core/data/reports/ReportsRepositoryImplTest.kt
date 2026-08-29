package com.leanecorps.dapurjember.core.data.reports

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.domain.order.PaymentMethod
import com.leanecorps.dapurjember.core.testing.database.OrderEntityFixtures
import com.leanecorps.dapurjember.core.testing.database.seedOrderPrerequisites
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReportsRepositoryImplTest {

    private lateinit var db: DapurJemberDatabase
    private lateinit var repo: ReportsRepositoryImpl

    private val day = "2026-08-29"

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DapurJemberDatabase::class.java)
            .allowMainThreadQueries().build()
        db.seedOrderPrerequisites()
        repo = ReportsRepositoryImpl(db.reportsDao())

        // A paid order: 2 lines (Nasi Goreng ×2 @15_000, and a second item), one CASH + one EWALLET payment.
        db.orderDao().upsert(
            OrderEntityFixtures.order(id = "o1", state = "PAID", businessDay = day)
                .copy(totalMinor = 45_000, guestCount = 3),
        )
        db.orderLineDao().upsert(OrderEntityFixtures.orderLine(id = "l1", orderId = "o1", qty = 2))
        db.orderLineDao().upsert(
            OrderEntityFixtures.orderLine(id = "l2", orderId = "o1", qty = 1)
                .copy(itemNameSnapshot = "Es Teh", unitPriceSnapshotMinor = 15_000),
        )
        db.paymentDao().insert(
            OrderEntityFixtures.payment(id = "p1", orderId = "o1", method = "CASH", amountMinor = 30_000),
        )
        db.paymentDao().insert(
            OrderEntityFixtures.payment(id = "p2", orderId = "o1", method = "EWALLET", amountMinor = 15_000),
        )
        db.discountDao().insert(OrderEntityFixtures.discount(id = "d1", orderId = "o1", computedMinor = 5_000))

        // A voided order on the same day — must not count toward revenue.
        db.orderDao().upsert(
            OrderEntityFixtures.order(id = "o2", state = "VOIDED", businessDay = day).copy(totalMinor = 99_000),
        )
        // An order on a different day — must be excluded entirely.
        db.orderDao().upsert(
            OrderEntityFixtures.order(id = "o3", state = "PAID", businessDay = "2026-08-28").copy(totalMinor = 12_345),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `dailySummary aggregates only that day's paid orders`() = runTest {
        val summary = repo.dailySummary(day)

        assertEquals(1, summary.orderCount)
        assertEquals(3, summary.covers)
        assertEquals(45_000L, summary.grossRevenue.minor)
        assertEquals(45_000L, summary.averageTicket.minor)
        assertEquals(
            listOf(PaymentMethod.CASH to 30_000L, PaymentMethod.EWALLET to 15_000L),
            summary.paymentMix.map { it.method to it.amount.minor },
        )
        assertEquals(1, summary.discountCount)
        assertEquals(5_000L, summary.discountTotal.minor)
        assertEquals(1, summary.voidedOrders)
    }

    @Test
    fun `salesByItem ranks items by gross revenue`() = runTest {
        val rows = repo.salesByItem(day)

        assertEquals(
            listOf("Nasi Goreng Ayam" to 30_000L, "Es Teh" to 15_000L),
            rows.map { it.name to it.gross.minor },
        )
        assertEquals(2, rows.first().quantity)
    }
}
