package com.leanecorps.dapurjember.feature.order

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.domain.order.DiscountKind
import com.leanecorps.dapurjember.core.domain.order.OrderState
import com.leanecorps.dapurjember.core.domain.order.VoidReason

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
    val lineAction: LineActionUiState? = null,
    val discount: DiscountUiState? = null,
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

/**
 * The line-detail / void sheet (S07). [needsStepUp] is decided in the domain, so the PIN
 * field appears only when the signed-in staff genuinely lacks the permission (FR-A3).
 */
data class LineActionUiState(
    val lineId: String,
    val lineName: String,
    val sent: Boolean,
    val reason: VoidReason = VoidReason.WRONG_ORDER,
    val note: String = "",
    val needsStepUp: Boolean = false,
    val pin: String = "",
    val pinRejected: Boolean = false,
) {
    val canVoid: Boolean get() = !needsStepUp || pin.length >= MIN_PIN

    /**
     * The reason written to `order_line.void_reason` and the audit log. Stored as the stable
     * enum name plus any free text — never a translated label, so an audit report written in
     * one language still reads correctly in another (FR-O4).
     */
    val storedReason: String
        get() = if (note.isBlank()) reason.name else "${reason.name} — ${note.trim()}"
}

/** The discount sheet (S11). Percent is entered in whole percent, fixed in minor units. */
data class DiscountUiState(
    val kind: DiscountKind = DiscountKind.PERCENT,
    val valueText: String = "",
    val reason: String = "",
    val needsStepUp: Boolean = false,
    val pin: String = "",
    val pinRejected: Boolean = false,
) {
    /** Basis points for PERCENT, minor units for FIXED — matches `ApplyDiscountParams.value`. */
    val value: Long?
        get() = when (kind) {
            DiscountKind.PERCENT -> valueText.trim().toDoubleOrNull()
                ?.takeIf { it > 0.0 && it <= MAX_PERCENT }
                ?.let { (it * BASIS_POINTS_PER_PERCENT).toLong() }

            DiscountKind.FIXED -> valueText.trim().toLongOrNull()?.takeIf { it > 0 }
        }

    val canApply: Boolean
        get() = value != null && reason.isNotBlank() && (!needsStepUp || pin.length >= MIN_PIN)
}

private const val MIN_PIN = 4
private const val MAX_PERCENT = 100.0
private const val BASIS_POINTS_PER_PERCENT = 100

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
