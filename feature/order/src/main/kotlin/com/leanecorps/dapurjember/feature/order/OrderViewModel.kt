package com.leanecorps.dapurjember.feature.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.domain.auth.AuthoriseUseCase
import com.leanecorps.dapurjember.core.domain.auth.Permission
import com.leanecorps.dapurjember.core.domain.menu.MenuBoardItem
import com.leanecorps.dapurjember.core.domain.menu.MenuItemWithVariants
import com.leanecorps.dapurjember.core.domain.menu.MenuRepository
import com.leanecorps.dapurjember.core.domain.menu.ModifierGroupWithModifiers
import com.leanecorps.dapurjember.core.domain.menu.ObserveMenuUseCase
import com.leanecorps.dapurjember.core.domain.order.AddLineParams
import com.leanecorps.dapurjember.core.domain.order.ApplyDiscountParams
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    private val authorise: AuthoriseUseCase,
) : ViewModel() {

    private val orderId: String = requireNotNull(savedStateHandle[ORDER_ID_ARG])
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val picker = MutableStateFlow<ModifierPickerUiState?>(null)
    private val lineAction = MutableStateFlow<LineActionUiState?>(null)
    private val discount = MutableStateFlow<DiscountUiState?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val board = selectedCategoryId.flatMapLatest { categoryId ->
        if (categoryId == null) flowOf(emptyList()) else menuRepository.observeMenuBoard(categoryId)
    }

    private val core = combine(
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
    }

    val uiState: StateFlow<OrderUiState> =
        combine(core, picker, lineAction, discount) { base, pickerState, action, discountState ->
            base.copy(picker = pickerState, lineAction = action, discount = discountState)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), OrderUiState())

    fun selectCategory(categoryId: String) {
        selectedCategoryId.value = categoryId
    }

    fun addTile(tile: BoardTileUi) {
        viewModelScope.launch {
            val detail = menuRepository.observeItemWithVariants(tile.itemId).first() ?: return@launch
            val groups = menuRepository.observeItemModifierGroups(tile.itemId).first()
            if (detail.variants.size <= 1 && groups.isEmpty()) {
                val variantId = detail.variants.firstOrNull()?.id ?: return@launch
                addLine(variantId, emptyList())
            } else {
                picker.value = buildPicker(detail, groups)
            }
        }
    }

    fun pickVariant(variantId: String) = picker.update { it?.copy(selectedVariantId = variantId) }

    fun toggleModifier(groupId: String, modifierId: String) = picker.update { current ->
        current ?: return@update null
        val group = current.groups.first { it.id == groupId }
        val selected = current.selectedModifierIds
        val next = when {
            modifierId in selected -> selected - modifierId
            group.singleSelect -> selected - group.modifierIds.toSet() + modifierId
            else -> selected + modifierId
        }
        current.copy(selectedModifierIds = next)
    }

    fun dismissPicker() {
        picker.value = null
    }

    fun confirmPicker() {
        val current = picker.value ?: return
        if (!current.canConfirm) return
        viewModelScope.launch {
            addLine(current.selectedVariantId, current.selectedModifierIds.toList())
            picker.value = null
        }
    }

    private suspend fun addLine(variantId: String, modifierIds: List<String>) {
        val staffId = sessionRepository.currentSession()?.staffId ?: return
        orderRepository.addLine(
            AddLineParams(
                orderId = orderId,
                menuVariantId = variantId,
                quantity = 1,
                addedByStaffId = staffId,
                modifierIds = modifierIds,
            ),
        )
    }

    private fun buildPicker(
        detail: MenuItemWithVariants,
        groups: List<ModifierGroupWithModifiers>,
    ): ModifierPickerUiState {
        val variants = detail.variants.sortedBy { it.sortOrder }
            .map { PickerVariantUi(it.id, it.name, it.price.minor) }
        val pickerGroups = groups.map { g ->
            PickerGroupUi(
                id = g.group.id,
                name = g.group.name,
                required = g.group.required,
                singleSelect = g.group.singleSelect,
                minSelect = g.group.minSelect,
                maxSelect = g.group.maxSelect,
                modifiers = g.modifiers.map { PickerModifierUi(it.id, it.name, it.priceDelta.minor) },
            )
        }
        val defaults = groups.flatMap { g -> g.modifiers.filter { it.defaultSelected }.map { it.id } }.toSet()
        return ModifierPickerUiState(
            itemName = detail.item.name,
            variants = variants,
            selectedVariantId = variants.first().id,
            groups = pickerGroups,
            selectedModifierIds = defaults,
        )
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

    // --- Void a line (S07 / FR-O4), with step-up authorisation (FR-A3) ---

    fun openLineAction(line: OrderLineUi) {
        viewModelScope.launch {
            lineAction.value = LineActionUiState(
                lineId = line.id,
                lineName = line.name,
                sent = line.sent,
                // Only a *sent* line is privileged; an unsent line is still a draft (FR-A3).
                needsStepUp = line.sent && !authorise.currentUserCan(Permission.VOID_SENT_LINE),
            )
        }
    }

    fun editLineAction(transform: (LineActionUiState) -> LineActionUiState) {
        lineAction.value = lineAction.value?.let(transform)
    }

    fun dismissLineAction() {
        lineAction.value = null
    }

    fun confirmVoid() {
        val action = lineAction.value ?: return
        if (!action.canVoid) return
        viewModelScope.launch {
            val actorId = authorise.actorFor(Permission.VOID_SENT_LINE, action.pin.takeIf { it.isNotBlank() })
            if (actorId == null) {
                lineAction.value = action.copy(pin = "", error = "That PIN cannot authorise a void.")
                return@launch
            }
            orderRepository.voidLine(action.lineId, action.storedReason, actorId)
            lineAction.value = null
        }
    }

    // --- Discount (S11 / FR-P4) ---

    fun openDiscount() {
        viewModelScope.launch {
            discount.value = DiscountUiState(
                needsStepUp = !authorise.currentUserCan(Permission.APPLY_DISCOUNT),
            )
        }
    }

    fun editDiscount(transform: (DiscountUiState) -> DiscountUiState) {
        discount.value = discount.value?.let(transform)
    }

    fun dismissDiscount() {
        discount.value = null
    }

    fun confirmDiscount() {
        val draft = discount.value ?: return
        val value = draft.value
        if (!draft.canApply || value == null) return
        viewModelScope.launch {
            val actorId = authorise.actorFor(Permission.APPLY_DISCOUNT, draft.pin.takeIf { it.isNotBlank() })
            if (actorId == null) {
                discount.value = draft.copy(pin = "", error = "That PIN cannot authorise a discount.")
                return@launch
            }
            orderRepository.applyDiscount(
                ApplyDiscountParams(
                    orderId = orderId,
                    kind = draft.kind,
                    value = value,
                    reason = draft.reason.trim(),
                    authorisedByStaffId = actorId,
                ),
            )
            discount.value = null
        }
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
    name = buildString {
        append(if (variantName == "Regular") itemName else "$itemName ($variantName)")
        if (modifiers.isNotEmpty()) {
            append(" · ")
            append(modifiers.joinToString(", ") { m -> m.name })
        }
    },
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
