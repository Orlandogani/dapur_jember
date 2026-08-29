package com.leanecorps.dapurjember.core.data.menu

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.data.database.entity.CategoryEntity
import com.leanecorps.dapurjember.core.data.database.entity.MenuItemEntity
import com.leanecorps.dapurjember.core.data.database.entity.MenuVariantEntity
import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.MenuVariant

// --- entity -> domain ------------------------------------------------------------------

internal fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    sortOrder = sortOrder,
    colourHex = colourHex,
    active = active,
)

internal fun MenuItemEntity.toDomain() = MenuItem(
    id = id,
    categoryId = categoryId,
    name = name,
    description = description,
    imagePath = imagePath,
    sortOrder = sortOrder,
    available = available,
    taxExempt = taxExempt,
    trackStock = trackStock,
)

internal fun MenuVariantEntity.toDomain() = MenuVariant(
    id = id,
    menuItemId = menuItemId,
    name = name,
    price = Money(priceMinor),
    sku = sku,
    sortOrder = sortOrder,
)

// --- domain -> entity ----------------------------------------------------------------------
// [existing] null => a fresh row (revision 1, createdAt = now); otherwise an update that
// preserves createdAt and bumps revision.

internal fun Category.toEntity(existing: CategoryEntity?, now: Long, deviceId: String) =
    CategoryEntity(
        id = id,
        name = name,
        sortOrder = sortOrder,
        colourHex = colourHex,
        active = active,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
        deletedAt = existing?.deletedAt,
        deviceId = existing?.deviceId ?: deviceId,
        revision = (existing?.revision ?: 0) + 1,
    )

internal fun MenuItem.toEntity(existing: MenuItemEntity?, now: Long, deviceId: String) =
    MenuItemEntity(
        id = id,
        categoryId = categoryId,
        name = name,
        description = description,
        imagePath = imagePath,
        sortOrder = sortOrder,
        available = available,
        taxExempt = taxExempt,
        trackStock = trackStock,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
        deletedAt = existing?.deletedAt,
        deviceId = existing?.deviceId ?: deviceId,
        revision = (existing?.revision ?: 0) + 1,
    )

internal fun MenuVariant.toEntity(existing: MenuVariantEntity?, now: Long, deviceId: String) =
    MenuVariantEntity(
        id = id,
        menuItemId = menuItemId,
        name = name,
        priceMinor = price.minor,
        sku = sku,
        sortOrder = sortOrder,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
        deletedAt = existing?.deletedAt,
        deviceId = existing?.deviceId ?: deviceId,
        revision = (existing?.revision ?: 0) + 1,
    )
