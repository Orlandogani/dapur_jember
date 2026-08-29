package com.leanecorps.dapurjember.feature.menu.modifiers

import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.menu.Modifier
import com.leanecorps.dapurjember.core.domain.menu.ModifierGroup
import com.leanecorps.dapurjember.core.domain.menu.ModifierGroupWithModifiers
import java.math.BigDecimal
import java.math.RoundingMode

data class ModifierGroupsUiState(
    val loading: Boolean = true,
    val currencyMinorUnits: Int = 0,
    val groups: List<ModifierGroupRowUi> = emptyList(),
    val editor: ModifierGroupDraft? = null,
)

data class ModifierGroupRowUi(val id: String, val name: String, val summary: String)

data class ModifierGroupDraft(
    val id: String? = null,
    val name: String = "",
    val required: Boolean = false,
    val minSelectText: String = "0",
    val maxSelectText: String = "1",
    val modifiers: List<ModifierRowDraft> = listOf(ModifierRowDraft()),
) {
    val minSelect: Int? get() = minSelectText.trim().toIntOrNull()?.takeIf { it >= 0 }
    val maxSelect: Int? get() = maxSelectText.trim().toIntOrNull()?.takeIf { it >= 0 }
    val isNew: Boolean get() = id == null

    val canSave: Boolean
        get() = name.isNotBlank() &&
            minSelect != null &&
            maxSelect != null &&
            modifiers.isNotEmpty() &&
            modifiers.all { it.name.isNotBlank() && it.priceDeltaMinorFor(0) != null }
}

data class ModifierRowDraft(
    val id: String = UuidV7.generate(),
    val name: String = "",
    val priceDeltaText: String = "0",
    val defaultSelected: Boolean = false,
) {
    fun priceDeltaMinorFor(minorUnits: Int): Long? {
        val value = priceDeltaText.trim().replace(',', '.').toBigDecimalOrNull() ?: return null
        return value.movePointRight(minorUnits).setScale(0, RoundingMode.HALF_UP).toLong()
    }
}

internal fun ModifierGroupWithModifiers.toRowUi() = ModifierGroupRowUi(
    id = group.id,
    name = group.name,
    summary = "${modifiers.size} options · " +
        (if (group.required) "required" else "optional") +
        " · choose ${group.minSelect}–${if (group.maxSelect == 0) "any" else group.maxSelect}",
)

internal fun ModifierGroupWithModifiers.toDraft(minorUnits: Int) = ModifierGroupDraft(
    id = group.id,
    name = group.name,
    required = group.required,
    minSelectText = group.minSelect.toString(),
    maxSelectText = group.maxSelect.toString(),
    modifiers = modifiers.map { m ->
        ModifierRowDraft(
            id = m.id,
            name = m.name,
            priceDeltaText = m.priceDelta.majorUnits(minorUnits),
            defaultSelected = m.defaultSelected,
        )
    }.ifEmpty { listOf(ModifierRowDraft()) },
)

internal fun ModifierGroupDraft.toGroup(newId: String) = ModifierGroup(
    id = id ?: newId,
    name = name.trim(),
    minSelect = minSelect ?: 0,
    maxSelect = maxSelect ?: 1,
    required = required,
)

internal fun ModifierGroupDraft.toModifiers(groupId: String, minorUnits: Int): List<Modifier> =
    modifiers.mapIndexed { index, row ->
        Modifier(
            id = row.id,
            modifierGroupId = groupId,
            name = row.name.trim(),
            priceDelta = Money(row.priceDeltaMinorFor(minorUnits) ?: 0L),
            sortOrder = index,
            defaultSelected = row.defaultSelected,
        )
    }

private fun Money.majorUnits(minorUnits: Int): String =
    BigDecimal.valueOf(minor).movePointLeft(minorUnits).stripTrailingZeros().toPlainString()
