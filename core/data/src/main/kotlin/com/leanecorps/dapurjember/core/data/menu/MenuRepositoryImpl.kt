package com.leanecorps.dapurjember.core.data.menu

import androidx.room.withTransaction
import com.leanecorps.dapurjember.core.common.time.TimeProvider
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.ChangeOp
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.database.dao.CategoryDao
import com.leanecorps.dapurjember.core.data.database.dao.MenuItemDao
import com.leanecorps.dapurjember.core.data.database.dao.MenuVariantDao
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.MenuBoardItem
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.MenuItemWithVariants
import com.leanecorps.dapurjember.core.domain.menu.MenuRepository
import com.leanecorps.dapurjember.core.domain.menu.MenuVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class MenuRepositoryImpl @Inject constructor(
    private val db: DapurJemberDatabase,
    private val categoryDao: CategoryDao,
    private val menuItemDao: MenuItemDao,
    private val menuVariantDao: MenuVariantDao,
    private val changeLog: ChangeLogRecorder,
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

    override suspend fun setItemAvailability(itemId: String, available: Boolean) = db.withTransaction {
        val existing = menuItemDao.getById(itemId) ?: return@withTransaction
        val now = time.nowMillis()
        menuItemDao.upsert(
            existing.copy(available = available, updatedAt = now, revision = existing.revision + 1),
        )
        changeLog.record("menu_item", itemId, ChangeOp.UPDATE, now)
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
