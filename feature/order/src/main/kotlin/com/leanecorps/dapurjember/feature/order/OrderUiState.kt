package com.leanecorps.dapurjember.feature.order

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.order.OrderState

data class OrderUiState(
    val loading: Boolean = true,
    val orderId: String = "",
    val orderNumber: String = "",
    val guestCount: Int = 0,
    val state: OrderState = OrderState.DRAFT,
    val categories: List<CategoryTabUi> = emptyList(),
    val selectedCategoryId: String? = null,
    val board: List<BoardTileUi> = emptyList(),
    val lines: List<OrderLineUi> = emptyList(),
    val totals: TotalsUi = TotalsUi(),
    val picker: ModifierPickerUiState? = null,
) {
    val canSend: Boolean get() = lines.any { !it.voided && !it.sent }
    val canPay: Boolean get() = lines.any { !it.voided } && state != OrderState.PAID && state != OrderState.CLOSED
}

/** The add-item sheet (S06): pick a variant and satisfy each modifier group before the line is added. */
data class ModifierPickerUiState(
    val itemName: String,
    val variants: List<PickerVariantUi>,
    val selectedVariantId: String,
    val groups: List<PickerGroupUi>,
    val selectedModifierIds: Set<String>,
) {
    val canConfirm: Boolean
        get() = groups.all { group ->
            val chosen = group.modifierIds.count { it in selectedModifierIds }
            chosen >= group.minSelect && (group.maxSelect == 0 || chosen <= group.maxSelect)
        }
}

data class PickerVariantUi(val id: String, val name: String, val priceMinor: Long)

data class PickerGroupUi(
    val id: String,
    val name: String,
    val required: Boolean,
    val singleSelect: Boolean,
    val minSelect: Int,
    val maxSelect: Int,
    val modifiers: List<PickerModifierUi>,
) {
    val modifierIds: List<String> get() = modifiers.map { it.id }
}

data class PickerModifierUi(val id: String, val name: String, val priceDeltaMinor: Long)

data class CategoryTabUi(val id: String, val name: String)

data class BoardTileUi(
    val itemId: String,
    val name: String,
    val available: Boolean,
    val priceMinor: Long?,
    /** The variant to add on tap, or null when the item needs a variant picker. */
    val addVariantId: String?,
)

data class OrderLineUi(
    val id: String,
    val name: String,
    val quantity: Int,
    val lineTotalMinor: Long,
    val sent: Boolean,
    val voided: Boolean,
    val note: String?,
)

data class TotalsUi(
    val subtotalMinor: Long = 0,
    val discountMinor: Long = 0,
    val serviceChargeMinor: Long = 0,
    val taxMinor: Long = 0,
    val totalMinor: Long = 0,
)

internal fun Money.minorOrZero(): Long = minor
