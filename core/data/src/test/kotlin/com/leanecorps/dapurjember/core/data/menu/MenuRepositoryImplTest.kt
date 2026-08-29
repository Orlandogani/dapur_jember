package com.leanecorps.dapurjember.core.data.menu

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.MenuVariant
import com.leanecorps.dapurjember.core.testing.FakeTimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MenuRepositoryImplTest {

    private lateinit var db: DapurJemberDatabase
    private lateinit var repo: MenuRepositoryImpl
    private val time = FakeTimeProvider(now = 1_000L)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DapurJemberDatabase::class.java)
            .allowMainThreadQueries().build()
        val deviceIds = DeviceIdProvider(context)
        repo = MenuRepositoryImpl(
            db = db,
            categoryDao = db.categoryDao(),
            menuItemDao = db.menuItemDao(),
            menuVariantDao = db.menuVariantDao(),
            changeLog = ChangeLogRecorder(db.changeLogDao(), deviceIds),
            time = time,
            deviceIds = deviceIds,
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun changeOps(): List<String> =
        db.changeLogDao().observeUnsynced().first().map { "${it.entityType}:${it.op}" }

    @Test
    fun `upsertCategory persists the domain object and logs an INSERT`() = runTest {
        repo.upsertCategory(Category(id = "c1", name = "Drinks", sortOrder = 1))

        assertEquals(
            Category(id = "c1", name = "Drinks", sortOrder = 1),
            repo.observeCategories().first().single(),
        )
        assertEquals(listOf("category:INSERT"), changeOps())
    }

    @Test
    fun `re-upserting a category logs an UPDATE, bumps revision, keeps createdAt`() = runTest {
        repo.upsertCategory(Category(id = "c1", name = "Drinks"))
        time.now = 2_000L
        repo.upsertCategory(Category(id = "c1", name = "Beverages"))

        assertEquals("Beverages", repo.observeCategories().first().single().name)
        val entity = db.categoryDao().getById("c1")!!
        assertEquals(2, entity.revision)
        assertEquals(1_000L, entity.createdAt)
        assertEquals(2_000L, entity.updatedAt)
        assertEquals(listOf("category:INSERT", "category:UPDATE"), changeOps())
    }

    @Test
    fun `setItemAvailability flips the sold-out flag and logs an UPDATE`() = runTest {
        repo.upsertCategory(Category(id = "c1", name = "Food"))
        time.advanceBy(1)
        repo.upsertItem(MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng"))
        time.advanceBy(1)
        repo.setItemAvailability("i1", available = false)

        assertEquals(false, repo.getItem("i1")!!.available)
        assertEquals(listOf("category:INSERT", "menu_item:INSERT", "menu_item:UPDATE"), changeOps())
    }

    @Test
    fun `softDeleteItem hides the item and logs a DELETE`() = runTest {
        repo.upsertCategory(Category(id = "c1", name = "Food"))
        time.advanceBy(1)
        repo.upsertItem(MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng"))
        time.advanceBy(1)
        repo.softDeleteItem("i1")

        assertEquals(emptyList<MenuItem>(), repo.observeItems("c1").first())
        assertNull(repo.getItem("i1"))
        assertEquals("menu_item:DELETE", changeOps().last())
    }

    @Test
    fun `saveItemWithVariants upserts kept variants and soft-deletes the rest in one shot`() = runTest {
        repo.upsertCategory(Category(id = "c1", name = "Food"))
        val small = MenuVariant(id = "v1", menuItemId = "i1", name = "Small", price = Money(12_000))
        val large = MenuVariant(id = "v2", menuItemId = "i1", name = "Large", price = Money(18_000))
        repo.saveItemWithVariants(MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng"), listOf(small, large))
        time.advanceBy(1)

        // Rename the item, keep Small (re-priced), drop Large, add Family.
        repo.saveItemWithVariants(
            MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng Spesial"),
            listOf(
                small.copy(price = Money(13_000)),
                MenuVariant(id = "v3", menuItemId = "i1", name = "Family", price = Money(30_000)),
            ),
        )

        val detail = repo.observeItemWithVariants("i1").first()!!
        assertEquals("Nasi Goreng Spesial", detail.item.name)
        assertEquals(listOf("Family" to 30_000L, "Small" to 13_000L), detail.variants.map { it.name to it.price.minor })
        assertNull(db.menuVariantDao().getById("v2"))
        assertEquals("menu_variant:DELETE", changeOps().last())
    }

    @Test
    fun `observeItemWithVariants combines the item with its variants`() = runTest {
        repo.upsertCategory(Category(id = "c1", name = "Food"))
        repo.upsertItem(MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng"))
        repo.upsertVariant(MenuVariant(id = "v1", menuItemId = "i1", name = "Regular", price = Money(15_000)))

        val detail = repo.observeItemWithVariants("i1").first()!!
        assertEquals("Nasi Goreng", detail.item.name)
        assertEquals(listOf(Money(15_000)), detail.variants.map { it.price })
    }
}
