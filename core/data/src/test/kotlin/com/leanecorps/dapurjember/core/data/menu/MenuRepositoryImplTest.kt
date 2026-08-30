package com.leanecorps.dapurjember.core.data.menu

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.data.database.AuditLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.MenuVariant
import com.leanecorps.dapurjember.core.domain.menu.Modifier
import com.leanecorps.dapurjember.core.domain.menu.ModifierGroup
import com.leanecorps.dapurjember.core.testing.FakeTimeProvider
import com.leanecorps.dapurjember.core.testing.database.OperationalEntityFixtures
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
            modifierGroupDao = db.modifierGroupDao(),
            modifierDao = db.modifierDao(),
            itemModifierGroupDao = db.itemModifierGroupDao(),
            changeLog = ChangeLogRecorder(db.changeLogDao(), deviceIds),
            auditLog = AuditLogRecorder(db.auditLogDao()),
            time = time,
            deviceIds = deviceIds,
        )
    }

    @After
    fun tearDown() = db.close()

    /** `audit_log.actor_staff_id` is a real FK, so the actor has to exist before we can log. */
    private suspend fun seedActor() = db.staffDao().upsert(OperationalEntityFixtures.staff(id = ACTOR))

    private suspend fun priceEdits() = db.auditLogDao().observeRecent(limit = 50).first()
        .filter { it.action == "PRICE_EDIT" }

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
        seedActor()
        repo.upsertCategory(Category(id = "c1", name = "Food"))
        val small = MenuVariant(id = "v1", menuItemId = "i1", name = "Small", price = Money(12_000))
        val large = MenuVariant(id = "v2", menuItemId = "i1", name = "Large", price = Money(18_000))
        repo.saveItemWithVariants(
            MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng"),
            listOf(small, large),
            actorStaffId = ACTOR,
        )
        time.advanceBy(1)

        // Rename the item, keep Small (re-priced), drop Large, add Family.
        repo.saveItemWithVariants(
            MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng Spesial"),
            listOf(
                small.copy(price = Money(13_000)),
                MenuVariant(id = "v3", menuItemId = "i1", name = "Family", price = Money(30_000)),
            ),
            actorStaffId = ACTOR,
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

    @Test
    fun `saveModifierGroup keeps chosen modifiers and soft-deletes the rest`() = runTest {
        repo.saveModifierGroup(
            ModifierGroup(id = "g1", name = "Spice level", minSelect = 1, maxSelect = 1, required = true),
            listOf(
                Modifier(id = "m1", modifierGroupId = "g1", name = "Mild"),
                Modifier(id = "m2", modifierGroupId = "g1", name = "Hot", priceDelta = Money(2_000)),
            ),
        )
        time.advanceBy(1)
        repo.saveModifierGroup(
            ModifierGroup(id = "g1", name = "Spice", minSelect = 1, maxSelect = 1, required = true),
            listOf(Modifier(id = "m1", modifierGroupId = "g1", name = "Mild")),
        )

        val detail = repo.observeModifierGroup("g1").first()!!
        assertEquals("Spice", detail.group.name)
        assertEquals(listOf("Mild"), detail.modifiers.map { it.name })
        assertEquals("modifier:DELETE", changeOps().last())
    }

    @Test
    fun `setItemModifierGroups attaches, reorders and detaches links, re-adding a removed one`() = runTest {
        repo.upsertCategory(Category(id = "c1", name = "Food"))
        repo.upsertItem(MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng"))
        repo.saveModifierGroup(ModifierGroup(id = "g1", name = "Spice"), emptyList())
        repo.saveModifierGroup(ModifierGroup(id = "g2", name = "Add-ons"), emptyList())

        repo.setItemModifierGroups("i1", listOf("g1", "g2"))
        assertEquals(listOf("Spice", "Add-ons"), repo.observeItemModifierGroups("i1").first().map { it.group.name })

        repo.setItemModifierGroups("i1", listOf("g2")) // drop g1
        assertEquals(listOf("Add-ons"), repo.observeItemModifierGroups("i1").first().map { it.group.name })

        repo.setItemModifierGroups("i1", listOf("g1", "g2")) // re-add g1 (unique index survives soft delete)
        assertEquals(listOf("Spice", "Add-ons"), repo.observeItemModifierGroups("i1").first().map { it.group.name })
    }

    @Test
    fun `re-pricing an existing variant writes one PRICE_EDIT row naming the actor`() = runTest {
        seedActor()
        repo.upsertCategory(Category(id = "c1", name = "Food"))
        val item = MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng")
        val regular = MenuVariant(id = "v1", menuItemId = "i1", name = "Regular", price = Money(25_000))
        repo.saveItemWithVariants(item, listOf(regular), actorStaffId = ACTOR)

        // Pricing a brand-new variant is data entry, not a price edit.
        assertEquals(emptyList<String>(), priceEdits().map { it.entityId })

        time.advanceBy(1)
        repo.saveItemWithVariants(item, listOf(regular.copy(price = Money(30_000))), actorStaffId = ACTOR)

        val logged = priceEdits().single()
        assertEquals(ACTOR, logged.actorStaffId)
        assertEquals("menu_variant", logged.entityType)
        assertEquals("v1", logged.entityId)
        assertEquals("""{"price_minor":25000}""", logged.beforeJson)
        assertEquals("""{"price_minor":30000}""", logged.afterJson)
    }

    @Test
    fun `renaming an item without touching the price writes no PRICE_EDIT row`() = runTest {
        seedActor()
        repo.upsertCategory(Category(id = "c1", name = "Food"))
        val regular = MenuVariant(id = "v1", menuItemId = "i1", name = "Regular", price = Money(25_000))
        repo.saveItemWithVariants(
            MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng"),
            listOf(regular),
            actorStaffId = ACTOR,
        )
        time.advanceBy(1)

        repo.saveItemWithVariants(
            MenuItem(id = "i1", categoryId = "c1", name = "Nasi Goreng Spesial"),
            listOf(regular),
            actorStaffId = ACTOR,
        )

        assertEquals(emptyList<String>(), priceEdits().map { it.entityId })
    }
}

private const val ACTOR = "staff-1"
