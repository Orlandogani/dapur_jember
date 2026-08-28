package com.leanecorps.dapurjember.core.data.database

import com.leanecorps.dapurjember.core.data.database.entity.CategoryEntity
import com.leanecorps.dapurjember.core.data.database.entity.ChangeLogEntity
import com.leanecorps.dapurjember.core.data.database.entity.ItemModifierGroupEntity
import com.leanecorps.dapurjember.core.data.database.entity.MenuItemEntity
import com.leanecorps.dapurjember.core.data.database.entity.MenuVariantEntity
import com.leanecorps.dapurjember.core.data.database.entity.ModifierEntity
import com.leanecorps.dapurjember.core.data.database.entity.ModifierGroupEntity
import com.leanecorps.dapurjember.core.data.database.entity.StoreProfileEntity

/** Test-data builders. Defaults are valid; override only what a test cares about. */
internal object Fixtures {

    private const val DEVICE = "test-device"

    fun category(
        id: String = "cat-1",
        name: String = "Rice",
        sortOrder: Int = 0,
        deletedAt: Long? = null,
    ) = CategoryEntity(
        id = id,
        name = name,
        sortOrder = sortOrder,
        colourHex = null,
        active = true,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun menuItem(
        id: String = "item-1",
        categoryId: String = "cat-1",
        name: String = "Nasi Goreng Ayam",
        sortOrder: Int = 0,
        deletedAt: Long? = null,
    ) = MenuItemEntity(
        id = id,
        categoryId = categoryId,
        name = name,
        description = null,
        imagePath = null,
        sortOrder = sortOrder,
        available = true,
        taxExempt = false,
        trackStock = true,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun menuVariant(
        id: String = "var-1",
        menuItemId: String = "item-1",
        name: String = "Regular",
        priceMinor: Long = 15_000,
        sortOrder: Int = 0,
        deletedAt: Long? = null,
    ) = MenuVariantEntity(
        id = id,
        menuItemId = menuItemId,
        name = name,
        priceMinor = priceMinor,
        sku = null,
        sortOrder = sortOrder,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun modifierGroup(
        id: String = "grp-1",
        name: String = "Spice level",
        deletedAt: Long? = null,
    ) = ModifierGroupEntity(
        id = id,
        name = name,
        minSelect = 0,
        maxSelect = 1,
        required = false,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun modifier(
        id: String = "mod-1",
        modifierGroupId: String = "grp-1",
        name: String = "Extra hot",
        sortOrder: Int = 0,
        deletedAt: Long? = null,
    ) = ModifierEntity(
        id = id,
        modifierGroupId = modifierGroupId,
        name = name,
        priceDeltaMinor = 0,
        sortOrder = sortOrder,
        defaultSelected = false,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun itemModifierGroup(
        id: String = "img-1",
        menuItemId: String = "item-1",
        modifierGroupId: String = "grp-1",
        sortOrder: Int = 0,
        deletedAt: Long? = null,
    ) = ItemModifierGroupEntity(
        id = id,
        menuItemId = menuItemId,
        modifierGroupId = modifierGroupId,
        sortOrder = sortOrder,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = deletedAt,
        deviceId = DEVICE,
    )

    fun storeProfile(
        id: String = "store-1",
        name: String = "Dapur Jember",
        currencyCode: String = "IDR",
    ) = StoreProfileEntity(
        id = id,
        name = name,
        address = null,
        phone = null,
        taxId = null,
        currencyCode = currencyCode,
        currencyMinorUnits = 0,
        taxRateBp = 1_100,
        taxInclusive = true,
        serviceChargeBp = 0,
        serviceChargeTaxable = false,
        roundingMode = "NONE",
        businessDayCutoffMin = 240,
        timezoneId = "Asia/Jakarta",
        receiptHeader = null,
        receiptFooter = null,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = null,
        deviceId = DEVICE,
    )

    fun changeLog(
        id: String = "chg-1",
        entityType: String = "menu_item",
        entityId: String = "item-1",
        op: String = "INSERT",
        timestamp: Long = 1L,
        syncedAt: Long? = null,
    ) = ChangeLogEntity(
        id = id,
        entityType = entityType,
        entityId = entityId,
        op = op,
        timestamp = timestamp,
        deviceId = DEVICE,
        syncedAt = syncedAt,
    )
}
