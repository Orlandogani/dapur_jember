package com.leanecorps.dapurjember.core.domain.menu

import com.leanecorps.dapurjember.core.common.money.Money

/**
 * A reusable modifier group (FR-M3) — "Spice level" defined once and attached to forty dishes.
 * [minSelect]/[maxSelect] bound how many modifiers a diner may pick; [maxSelect] `0` means no
 * upper limit. [required] forces at least one choice at order time.
 */
data class ModifierGroup(
    val id: String,
    val name: String,
    val minSelect: Int = 0,
    val maxSelect: Int = 1,
    val required: Boolean = false,
) {
    val singleSelect: Boolean get() = maxSelect == 1
}

data class Modifier(
    val id: String,
    val modifierGroupId: String,
    val name: String,
    val priceDelta: Money = Money.ZERO,
    val sortOrder: Int = 0,
    val defaultSelected: Boolean = false,
)

data class ModifierGroupWithModifiers(
    val group: ModifierGroup,
    val modifiers: List<Modifier>,
)
