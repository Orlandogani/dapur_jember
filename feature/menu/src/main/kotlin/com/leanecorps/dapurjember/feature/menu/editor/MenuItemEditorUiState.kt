package com.leanecorps.dapurjember.feature.menu.editor

import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.menu.MenuItem
import com.leanecorps.dapurjember.core.domain.menu.MenuItemWithVariants
import com.leanecorps.dapurjember.core.domain.menu.MenuVariant
import java.math.BigDecimal
import java.math.RoundingMode

data class MenuItemEditorState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val categories: List<CategoryOption> = emptyList(),
    val modifierGroups: List<ModifierGroupOption> = emptyList(),
    val currencyMinorUnits: Int = 0,
    val draft: MenuItemDraft = MenuItemDraft(),
    val done: Boolean = false,
) {
    val canSave: Boolean
        get() = draft.name.isNotBlank() &&
            draft.categoryId.isNotBlank() &&
            draft.variants.isNotEmpty() &&
            draft.variants.all { it.name.isNotBlank() && it.priceMinor(currencyMinorUnits) != null }
}

data class CategoryOption(val id: String, val name: String)

data class ModifierGroupOption(val id: String, val name: String)

data class MenuItemDraft(
    val id: String? = null,
    val name: String = "",
    val categoryId: String = "",
    val available: Boolean = true,
    val taxExempt: Boolean = false,
    val variants: List<VariantDraft> = listOf(VariantDraft(name = "Regular")),
    val modifierGroupIds: List<String> = emptyList(),
)

data class VariantDraft(
    val id: String = UuidV7.generate(),
    val name: String = "",
    val priceText: String = "",
) {
    /** Parses [priceText] as major units into minor units, or null when it is not a valid amount. */
    fun priceMinor(minorUnits: Int): Long? {
        val value = priceText.trim().replace(',', '.').toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
        return value?.movePointRight(minorUnits)?.setScale(0, RoundingMode.HALF_UP)?.longValueExact()
    }
}

internal fun MenuItemWithVariants.toDraft(minorUnits: Int) = MenuItemDraft(
    id = item.id,
    name = item.name,
    categoryId = item.categoryId,
    available = item.available,
    taxExempt = item.taxExempt,
    variants = variants
        .sortedBy { it.sortOrder }
        .map { VariantDraft(id = it.id, name = it.name, priceText = it.price.majorUnits(minorUnits)) },
)

internal fun MenuItemDraft.toItem(newId: String): MenuItem = MenuItem(
    id = id ?: newId,
    categoryId = categoryId,
    name = name.trim(),
    available = available,
    taxExempt = taxExempt,
)

internal fun MenuItemDraft.toVariants(itemId: String, minorUnits: Int): List<MenuVariant> =
    variants.mapIndexed { index, v ->
        MenuVariant(
            id = v.id,
            menuItemId = itemId,
            name = v.name.trim(),
            price = Money(v.priceMinor(minorUnits) ?: 0L),
            sortOrder = index,
        )
    }

private fun Money.majorUnits(minorUnits: Int): String =
    BigDecimal.valueOf(minor).movePointLeft(minorUnits).stripTrailingZeros().toPlainString()
