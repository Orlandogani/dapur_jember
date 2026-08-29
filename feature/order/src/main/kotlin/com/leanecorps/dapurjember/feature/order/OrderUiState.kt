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
) {
    val canSend: Boolean get() = lines.any { !it.voided && !it.sent }
    val canPay: Boolean get() = lines.any { !it.voided } && state != OrderState.PAID && state != OrderState.CLOSED
}

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
