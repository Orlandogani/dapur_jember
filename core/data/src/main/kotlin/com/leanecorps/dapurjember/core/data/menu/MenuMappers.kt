package com.leanecorps.dapurjember.core.data.menu

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.data.database.entity.CategoryEntity
import com.leanecorps.dapurjember.core.data.database.entity.MenuItemEntity
import com.leanecorps.dapurjember.core.data.database.entity.MenuVariantEntity
import com.leanecorps.dapurjember.core.data.database.entity.ModifierEntity
import com.leanecorps.dapurjember.core.data.database.entity.ModifierGroupEntity
import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.MenuVariant
import com.leanecorps.dapurjember.core.domain.menu.Modifier
import com.leanecorps.dapurjember.core.domain.menu.ModifierGroup

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

internal fun ModifierGroupEntity.toDomain() = ModifierGroup(
    id = id,
    name = name,
    minSelect = minSelect,
    maxSelect = maxSelect,
    required = required,
)

internal fun ModifierEntity.toDomain() = Modifier(
    id = id,
    modifierGroupId = modifierGroupId,
    name = name,
    priceDelta = Money(priceDeltaMinor),
    sortOrder = sortOrder,
    defaultSelected = defaultSelected,
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

internal fun ModifierGroup.toEntity(existing: ModifierGroupEntity?, now: Long, deviceId: String) =
    ModifierGroupEntity(
        id = id,
        name = name,
        minSelect = minSelect,
        maxSelect = maxSelect,
        required = required,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
        deletedAt = existing?.deletedAt,
        deviceId = existing?.deviceId ?: deviceId,
        revision = (existing?.revision ?: 0) + 1,
    )

internal fun Modifier.toEntity(existing: ModifierEntity?, now: Long, deviceId: String) =
    ModifierEntity(
        id = id,
        modifierGroupId = modifierGroupId,
        name = name,
        priceDeltaMinor = priceDelta.minor,
        sortOrder = sortOrder,
        defaultSelected = defaultSelected,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
        deletedAt = existing?.deletedAt,
        deviceId = existing?.deviceId ?: deviceId,
        revision = (existing?.revision ?: 0) + 1,
    )
