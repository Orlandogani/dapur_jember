package com.leanecorps.dapurjember.core.domain.menu

import com.leanecorps.dapurjember.core.common.money.Money

/**
 * Menu domain models — what use cases and ViewModels work with. Clean of the persistence
 * envelope (createdAt/updatedAt/deletedAt/deviceId/revision live only on the Room entities).
 * `id` is a client-side UUIDv7, generated before the object is constructed.
 */

data class Category(
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
    val colourHex: String? = null,
    val active: Boolean = true,
)

data class MenuItem(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String? = null,
    val imagePath: String? = null,
    val sortOrder: Int = 0,
    /** The sold-out toggle (FR-M2). Unavailable items render disabled, not hidden. */
    val available: Boolean = true,
    val taxExempt: Boolean = false,
    val trackStock: Boolean = false,
)

data class MenuVariant(
    val id: String,
    val menuItemId: String,
    val name: String,
    val price: Money,
    val sku: String? = null,
    val sortOrder: Int = 0,
)

/** An item together with its variants — every item has at least one, even if called "Regular". */
data class MenuItemWithVariants(
    val item: MenuItem,
    val variants: List<MenuVariant>,
)

/** A grid tile on the order screen: item + its orderable variants. */
data class MenuBoardItem(
    val item: MenuItem,
    val variants: List<MenuVariant>,
) {
    /** The single variant to add on a one-tap add, or `null` if a picker is needed. */
    val singleVariant: MenuVariant? get() = variants.singleOrNull()
}
