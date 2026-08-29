package com.leanecorps.dapurjember.core.data.inventory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.data.database.AuditLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.inventory.BaseUnit
import com.leanecorps.dapurjember.core.domain.inventory.Ingredient
import com.leanecorps.dapurjember.core.domain.inventory.StockAdjustment
import com.leanecorps.dapurjember.core.domain.inventory.StockReason
import com.leanecorps.dapurjember.core.testing.FakeTimeProvider
import com.leanecorps.dapurjember.core.testing.database.OperationalEntityFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InventoryRepositoryImplTest {

    private lateinit var db: DapurJemberDatabase
    private lateinit var repo: InventoryRepositoryImpl
    private val time = FakeTimeProvider(now = 1_000L)

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DapurJemberDatabase::class.java)
            .allowMainThreadQueries().build()
        db.staffDao().upsert(OperationalEntityFixtures.staff())
        val deviceIds = DeviceIdProvider(context)
        repo = InventoryRepositoryImpl(
            db = db,
            ingredientDao = db.ingredientDao(),
            stockMovementDao = db.stockMovementDao(),
            changeLog = ChangeLogRecorder(db.changeLogDao(), deviceIds),
            auditLog = AuditLogRecorder(db.auditLogDao()),
            time = time,
            deviceIds = deviceIds,
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seedChicken() = repo.upsertIngredient(
        Ingredient(
            id = "chicken",
            name = "Chicken",
            baseUnit = BaseUnit.G,
            purchaseUnit = "kg",
            purchaseToBaseFactor = 1_000.0,
            currentStockBase = 0.0,
            avgCostPerBase = Money.ZERO,
            lowStockThresholdBase = 2_000.0,
        ),
    )

    @Test
    fun `a purchase raises stock, rolls the weighted-average cost, writes a movement and an audit row`() = runTest {
        seedChicken()
        repo.adjustStock(
            StockAdjustment(
                ingredientId = "chicken",
                qtyBaseDelta = 1_000.0,
                reason = StockReason.PURCHASE,
                staffId = "staff-1",
                unitCost = Money(50),
            ),
        )
        repo.adjustStock(
            StockAdjustment(
                ingredientId = "chicken",
                qtyBaseDelta = 1_000.0,
                reason = StockReason.PURCHASE,
                staffId = "staff-1",
                unitCost = Money(70),
            ),
        )

        val chicken = repo.getIngredient("chicken")!!
        assertEquals(2_000.0, chicken.currentStockBase, 0.0)
        assertEquals(Money(60), chicken.avgCostPerBase)
        assertEquals(2, repo.observeMovements("chicken").first().size)
        assertEquals("STOCK_ADJUST", db.auditLogDao().observeRecent(10).first().first().action)
    }

    @Test
    fun `waste lowers stock without touching the average, and low-stock surfaces it`() = runTest {
        seedChicken()
        repo.adjustStock(
            StockAdjustment("chicken", 3_000.0, StockReason.PURCHASE, "staff-1", Money(40)),
        )
        repo.adjustStock(
            StockAdjustment("chicken", -1_500.0, StockReason.WASTE, "staff-1"),
        )

        val chicken = repo.getIngredient("chicken")!!
        assertEquals(1_500.0, chicken.currentStockBase, 0.0)
        assertEquals(Money(40), chicken.avgCostPerBase)
        assertTrue(chicken.isLowStock)
        assertEquals(listOf("chicken"), repo.observeLowStock().first().map { it.id })
    }
}
