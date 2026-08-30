package com.leanecorps.dapurjember.core.data.menu

import androidx.room.withTransaction
import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.common.time.TimeProvider
import com.leanecorps.dapurjember.core.data.database.AuditLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeOp
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.database.dao.CategoryDao
import com.leanecorps.dapurjember.core.data.database.dao.ItemModifierGroupDao
import com.leanecorps.dapurjember.core.data.database.dao.MenuItemDao
import com.leanecorps.dapurjember.core.data.database.dao.MenuVariantDao
import com.leanecorps.dapurjember.core.data.database.dao.ModifierDao
import com.leanecorps.dapurjember.core.data.database.dao.ModifierGroupDao
import com.leanecorps.dapurjember.core.data.database.entity.ItemModifierGroupEntity
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.MenuBoardItem
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.MenuItemWithVariants
import com.leanecorps.dapurjember.core.domain.menu.MenuRepository
import com.leanecorps.dapurjember.core.domain.menu.MenuVariant
import com.leanecorps.dapurjember.core.domain.menu.Modifier
import com.leanecorps.dapurjember.core.domain.menu.ModifierGroup
import com.leanecorps.dapurjember.core.domain.menu.ModifierGroupWithModifiers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
internal class MenuRepositoryImpl @Inject constructor(
    private val db: DapurJemberDatabase,
    private val categoryDao: CategoryDao,
    private val menuItemDao: MenuItemDao,
    private val menuVariantDao: MenuVariantDao,
    private val modifierGroupDao: ModifierGroupDao,
    private val modifierDao: ModifierDao,
    private val itemModifierGroupDao: ItemModifierGroupDao,
    private val changeLog: ChangeLogRecorder,
    private val auditLog: AuditLogRecorder,
    private val time: TimeProvider,
    private val deviceIds: DeviceIdProvider,
) : MenuRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeItems(categoryId: String): Flow<List<MenuItem>> =
        menuItemDao.observeByCategory(categoryId).map { rows -> rows.map { it.toDomain() } }

    override fun observeItemWithVariants(itemId: String): Flow<MenuItemWithVariants?> =
        combine(
            menuItemDao.observeById(itemId),
            menuVariantDao.observeForItem(itemId),
        ) { item, variants ->
            item?.let { MenuItemWithVariants(it.toDomain(), variants.map { v -> v.toDomain() }) }
        }

    override fun observeMenuBoard(categoryId: String): Flow<List<MenuBoardItem>> =
        combine(
            menuItemDao.observeByCategory(categoryId),
            menuVariantDao.observeForCategory(categoryId),
        ) { items, variants ->
            val byItem = variants.groupBy { it.menuItemId }
            items.map { item ->
                MenuBoardItem(
                    item = item.toDomain(),
                    variants = byItem[item.id].orEmpty().map { it.toDomain() },
                )
            }
        }

    override suspend fun getItem(itemId: String): MenuItem? = menuItemDao.getById(itemId)?.toDomain()

    override suspend fun upsertCategory(category: Category) = db.withTransaction {
        val existing = categoryDao.getById(category.id)
        val now = time.nowMillis()
        categoryDao.upsert(category.toEntity(existing, now, deviceIds.deviceId()))
        changeLog.record("category", category.id, opFor(existing), now)
    }

    override suspend fun upsertItem(item: MenuItem) = db.withTransaction {
        val existing = menuItemDao.getById(item.id)
        val now = time.nowMillis()
        menuItemDao.upsert(item.toEntity(existing, now, deviceIds.deviceId()))
        changeLog.record("menu_item", item.id, opFor(existing), now)
    }

    override suspend fun upsertVariant(variant: MenuVariant) = db.withTransaction {
        val existing = menuVariantDao.getById(variant.id)
        val now = time.nowMillis()
        menuVariantDao.upsert(variant.toEntity(existing, now, deviceIds.deviceId()))
        changeLog.record("menu_variant", variant.id, opFor(existing), now)
    }

    override suspend fun saveItemWithVariants(
        item: MenuItem,
        variants: List<MenuVariant>,
        actorStaffId: String,
    ) = db.withTransaction {
        val now = time.nowMillis()
        val device = deviceIds.deviceId()

        val existingItem = menuItemDao.getById(item.id)
        menuItemDao.upsert(item.toEntity(existingItem, now, device))
        changeLog.record("menu_item", item.id, opFor(existingItem), now)

        val keepIds = variants.map { it.id }.toSet()
        variants.forEach { variant ->
            val existingVariant = menuVariantDao.getById(variant.id)
            menuVariantDao.upsert(variant.toEntity(existingVariant, now, device))
            changeLog.record("menu_variant", variant.id, opFor(existingVariant), now)

            // Only a change to an *existing* price is a price edit. Pricing a new variant is
            // just data entry, and auditing it would make a CSV import unreadable.
            if (existingVariant != null && existingVariant.priceMinor != variant.price.minor) {
                auditLog.record(
                    actorStaffId = actorStaffId,
                    action = "PRICE_EDIT",
                    entityType = "menu_variant",
                    entityId = variant.id,
                    at = now,
                    beforeJson = priceJson(existingVariant.priceMinor),
                    afterJson = priceJson(variant.price.minor),
                )
            }
        }
        menuVariantDao.getForItem(item.id)
            .filter { it.id !in keepIds }
            .forEach { removed ->
                menuVariantDao.softDelete(removed.id, now)
                changeLog.record("menu_variant", removed.id, ChangeOp.DELETE, now)
            }
    }

    override suspend fun setItemAvailability(itemId: String, available: Boolean) = db.withTransaction {
        val existing = menuItemDao.getById(itemId) ?: return@withTransaction
        val now = time.nowMillis()
        menuItemDao.upsert(
            existing.copy(available = available, updatedAt = now, revision = existing.revision + 1),
        )
        changeLog.record("menu_item", itemId, ChangeOp.UPDATE, now)
    }

    override fun observeModifierGroups(): Flow<List<ModifierGroup>> =
        modifierGroupDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeModifierGroup(groupId: String): Flow<ModifierGroupWithModifiers?> =
        combine(
            modifierGroupDao.observeAll(),
            modifierDao.observeForGroup(groupId),
        ) { groups, modifiers ->
            groups.firstOrNull { it.id == groupId }?.let { group ->
                ModifierGroupWithModifiers(group.toDomain(), modifiers.map { it.toDomain() })
            }
        }

    override suspend fun saveModifierGroup(group: ModifierGroup, modifiers: List<Modifier>) = db.withTransaction {
        val now = time.nowMillis()
        val device = deviceIds.deviceId()

        val existingGroup = modifierGroupDao.getById(group.id)
        modifierGroupDao.upsert(group.toEntity(existingGroup, now, device))
        changeLog.record("modifier_group", group.id, opFor(existingGroup), now)

        val keepIds = modifiers.map { it.id }.toSet()
        modifiers.forEach { modifier ->
            val existing = modifierDao.getById(modifier.id)
            modifierDao.upsert(modifier.copy(modifierGroupId = group.id).toEntity(existing, now, device))
            changeLog.record("modifier", modifier.id, opFor(existing), now)
        }
        modifierDao.getForGroup(group.id)
            .filter { it.id !in keepIds }
            .forEach { removed ->
                modifierDao.softDelete(removed.id, now)
                changeLog.record("modifier", removed.id, ChangeOp.DELETE, now)
            }
    }

    override suspend fun softDeleteModifierGroup(groupId: String) = softDelete("modifier_group", groupId) { id, now ->
        modifierGroupDao.softDelete(id, now)
    }

    override fun observeItemModifierGroups(itemId: String): Flow<List<ModifierGroupWithModifiers>> =
        combine(
            modifierGroupDao.observeForItem(itemId),
            modifierDao.observeForItem(itemId),
        ) { groups, modifiers ->
            val byGroup = modifiers.groupBy { it.modifierGroupId }
            groups.map { group ->
                ModifierGroupWithModifiers(
                    group.toDomain(),
                    byGroup[group.id].orEmpty().map { it.toDomain() },
                )
            }
        }

    override suspend fun setItemModifierGroups(itemId: String, groupIds: List<String>) = db.withTransaction {
        val now = time.nowMillis()
        val device = deviceIds.deviceId()
        val existing = itemModifierGroupDao.getAllForItem(itemId).associateBy { it.modifierGroupId }

        groupIds.forEachIndexed { index, groupId ->
            val link = existing[groupId]
            when {
                link == null -> {
                    val id = UuidV7.generate()
                    itemModifierGroupDao.insert(
                        ItemModifierGroupEntity(
                            id = id,
                            menuItemId = itemId,
                            modifierGroupId = groupId,
                            sortOrder = index,
                            createdAt = now,
                            updatedAt = now,
                            deviceId = device,
                        ),
                    )
                    changeLog.record("item_modifier_group", id, ChangeOp.INSERT, now)
                }

                link.deletedAt != null -> {
                    itemModifierGroupDao.restore(link.id, index, now)
                    changeLog.record("item_modifier_group", link.id, ChangeOp.UPDATE, now)
                }

                link.sortOrder != index -> {
                    itemModifierGroupDao.reorder(link.id, index, now)
                    changeLog.record("item_modifier_group", link.id, ChangeOp.UPDATE, now)
                }
            }
        }

        existing.values
            .filter { it.deletedAt == null && it.modifierGroupId !in groupIds }
            .forEach { stale ->
                itemModifierGroupDao.softDelete(stale.id, now)
                changeLog.record("item_modifier_group", stale.id, ChangeOp.DELETE, now)
            }
    }

    override suspend fun softDeleteCategory(categoryId: String) = softDelete("category", categoryId) { id, now ->
        categoryDao.softDelete(id, now)
    }

    override suspend fun softDeleteItem(itemId: String) = softDelete("menu_item", itemId) { id, now ->
        menuItemDao.softDelete(id, now)
    }

    override suspend fun softDeleteVariant(variantId: String) = softDelete("menu_variant", variantId) { id, now ->
        menuVariantDao.softDelete(id, now)
    }

    private suspend inline fun softDelete(
        entityType: String,
        entityId: String,
        crossinline delete: suspend (id: String, now: Long) -> Unit,
    ) = db.withTransaction {
        val now = time.nowMillis()
        delete(entityId, now)
        changeLog.record(entityType, entityId, ChangeOp.DELETE, now)
    }

    private fun opFor(existing: Any?): ChangeOp = if (existing == null) ChangeOp.INSERT else ChangeOp.UPDATE
}

/**
 * The before/after payload for a price edit. Hand-built rather than serialised: it is two
 * fields, and the audit log must stay readable years from now without a schema to consult.
 */
private fun priceJson(priceMinor: Long): String = """{"price_minor":$priceMinor}"""
