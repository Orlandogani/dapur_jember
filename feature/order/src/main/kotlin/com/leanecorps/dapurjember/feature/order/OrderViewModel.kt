package com.leanecorps.dapurjember.feature.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.domain.menu.MenuBoardItem
import com.leanecorps.dapurjember.core.domain.menu.MenuRepository
import com.leanecorps.dapurjember.core.domain.menu.ObserveMenuUseCase
import com.leanecorps.dapurjember.core.domain.order.AddLineParams
import com.leanecorps.dapurjember.core.domain.order.Order
import com.leanecorps.dapurjember.core.domain.order.OrderLine
import com.leanecorps.dapurjember.core.domain.order.OrderRepository
import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ORDER_ID_ARG = "orderId"

@HiltViewModel
class OrderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeMenu: ObserveMenuUseCase,
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val orderId: String = requireNotNull(savedStateHandle[ORDER_ID_ARG])
    private val selectedCategoryId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val board = selectedCategoryId.flatMapLatest { categoryId ->
        if (categoryId == null) flowOf(emptyList()) else menuRepository.observeMenuBoard(categoryId)
    }

    val uiState: StateFlow<OrderUiState> = combine(
        orderRepository.observeOrder(orderId),
        observeMenu(),
        selectedCategoryId,
        board,
    ) { order, sections, selected, boardItems ->
        val effectiveSelected = selected ?: sections.firstOrNull()?.category?.id
        if (selected == null && effectiveSelected != null) selectedCategoryId.value = effectiveSelected

        if (order == null) {
            OrderUiState(loading = false)
        } else {
            OrderUiState(
                loading = false,
                orderId = order.id,
                orderNumber = order.orderNumber,
                guestCount = order.guestCount,
                state = order.state,
                categories = sections.map { CategoryTabUi(it.category.id, it.category.name) },
                selectedCategoryId = effectiveSelected,
                board = boardItems.map { it.toTile() },
                lines = order.lines.map { it.toUi() },
                totals = order.toTotalsUi(),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), OrderUiState())

    fun selectCategory(categoryId: String) {
        selectedCategoryId.value = categoryId
    }

    fun addTile(tile: BoardTileUi) {
        val variantId = tile.addVariantId ?: return
        viewModelScope.launch {
            val staffId = sessionRepository.currentSession()?.staffId ?: return@launch
            orderRepository.addLine(
                AddLineParams(
                    orderId = orderId,
                    menuVariantId = variantId,
                    quantity = 1,
                    addedByStaffId = staffId,
                ),
            )
        }
    }

    fun increment(line: OrderLineUi) = setQuantity(line, line.quantity + 1)

    fun decrement(line: OrderLineUi) {
        if (line.quantity > 1) setQuantity(line, line.quantity - 1)
    }

    private fun setQuantity(line: OrderLineUi, quantity: Int) {
        viewModelScope.launch { orderRepository.setLineQuantity(line.id, quantity) }
    }

    fun send() {
        viewModelScope.launch { orderRepository.sendToKitchen(orderId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun MenuBoardItem.toTile() = BoardTileUi(
    itemId = item.id,
    name = item.name,
    available = item.available,
    priceMinor = singleVariant?.price?.minor,
    addVariantId = singleVariant?.id ?: variants.firstOrNull()?.id,
)

private fun OrderLine.toUi() = OrderLineUi(
    id = id,
    name = if (variantName == "Regular") itemName else "$itemName ($variantName)",
    quantity = quantity,
    lineTotalMinor = (effectiveUnitPrice * quantity).minor,
    sent = sentAt != null,
    voided = voided,
    note = note,
)

private fun Order.toTotalsUi() = TotalsUi(
    subtotalMinor = totals.subtotal.minor,
    discountMinor = totals.discount.minor,
    serviceChargeMinor = totals.serviceCharge.minor,
    taxMinor = totals.tax.minor,
    totalMinor = totals.total.minor,
)
