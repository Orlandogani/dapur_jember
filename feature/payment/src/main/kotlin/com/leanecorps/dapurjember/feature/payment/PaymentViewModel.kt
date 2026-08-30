package com.leanecorps.dapurjember.feature.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.common.money.splitEvenly
import com.leanecorps.dapurjember.core.domain.order.BillSplit
import com.leanecorps.dapurjember.core.domain.order.OrderRepository
import com.leanecorps.dapurjember.core.domain.order.OrderState
import com.leanecorps.dapurjember.core.domain.order.PaymentMethod
import com.leanecorps.dapurjember.core.domain.order.RecordPaymentParams
import com.leanecorps.dapurjember.core.domain.order.SettleOrderUseCase
import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val PAYMENT_ORDER_ID_ARG = "orderId"

data class PaymentUiState(
    val loading: Boolean = true,
    val totalMinor: Long = 0,
    val paidMinor: Long = 0,
    val settled: Boolean = false,
    val payments: List<PaidLineUi> = emptyList(),
    val lines: List<PayableLineUi> = emptyList(),
    val split: SplitUiState? = null,
) {
    val balanceMinor: Long get() = (totalMinor - paidMinor).coerceAtLeast(0)
}

data class PaidLineUi(val method: PaymentMethod, val amountMinor: Long, val changeMinor: Long)

data class PayableLineUi(val id: String, val name: String, val lineTotalMinor: Long)

enum class SplitMode { EVENLY, BY_ITEM }

/**
 * The split-bill sheet (S09). Splitting only *proposes* amounts; each part is then taken as
 * an ordinary partial payment, so a bill can be split three ways and paid three different
 * ways without any new persistence concept.
 */
data class SplitUiState(
    val mode: SplitMode = SplitMode.EVENLY,
    val ways: Int = 2,
    /** By-item: which guest index each order line is assigned to. Unassigned lines are shared. */
    val assignment: Map<String, Int> = emptyMap(),
    val parts: List<SplitPartUi> = emptyList(),
) {
    val partsTotalMinor: Long get() = parts.sumOf { it.amountMinor }
}

data class SplitPartUi(val label: String, val amountMinor: Long, val paid: Boolean = false)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
    private val sessionRepository: SessionRepository,
    private val settleOrder: SettleOrderUseCase,
) : ViewModel() {

    private val orderId: String = requireNotNull(savedStateHandle[PAYMENT_ORDER_ID_ARG])
    private val split = MutableStateFlow<SplitUiState?>(null)

    private val order = orderRepository.observeOrder(orderId)
        .map { order ->
            if (order == null) {
                PaymentUiState(loading = false)
            } else {
                PaymentUiState(
                    loading = false,
                    totalMinor = order.totals.total.minor,
                    paidMinor = order.amountPaid.minor,
                    settled = order.state == OrderState.PAID,
                    payments = order.payments.map {
                        PaidLineUi(it.method, it.amount.minor, it.change.minor)
                    },
                    lines = order.lines.filterNot { it.voided }.map { line ->
                        PayableLineUi(
                            id = line.id,
                            name = line.itemName,
                            lineTotalMinor = (line.effectiveUnitPrice * line.quantity).minor,
                        )
                    },
                )
            }
        }

    val uiState: StateFlow<PaymentUiState> = combine(order, split) { base, splitState ->
        base.copy(split = splitState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PaymentUiState())

    fun pay(method: PaymentMethod, amountMinor: Long, tenderedMinor: Long) {
        if (amountMinor <= 0) return
        viewModelScope.launch {
            val staffId = sessionRepository.currentSession()?.staffId ?: return@launch
            orderRepository.recordPayment(
                RecordPaymentParams(
                    orderId = orderId,
                    method = method,
                    amountMinor = amountMinor,
                    tenderedMinor = tenderedMinor,
                    staffId = staffId,
                ),
            )
            settleOrder(orderId)
        }
    }

    fun reprintReceipt() {
        viewModelScope.launch { orderRepository.reprintReceipt(orderId) }
    }

    // --- Split bill (S09) ---

    fun startSplit() {
        split.value = SplitUiState().let(::recomputed)
    }

    fun closeSplit() {
        split.value = null
    }

    fun setSplitMode(mode: SplitMode) = split.update { recomputed(it.copy(mode = mode)) }

    fun setSplitWays(ways: Int) = split.update { recomputed(it.copy(ways = ways.coerceIn(MIN_WAYS, MAX_WAYS))) }

    /** Cycles a line through the guests, then back to "shared" (unassigned). */
    fun cycleLineAssignment(lineId: String) = split.update { current ->
        val next = current.assignment[lineId]?.plus(1)?.takeIf { it < current.ways }
        recomputed(
            current.copy(
                assignment = if (next == null) current.assignment - lineId else current.assignment + (lineId to next),
            ),
        )
    }

    /**
     * Takes one part as an ordinary payment (FR-P3). The part is marked paid locally so the
     * cashier can see who has settled; the authoritative balance still comes from the order.
     */
    fun paySplitPart(index: Int, method: PaymentMethod) {
        val current = split.value ?: return
        val part = current.parts.getOrNull(index)?.takeIf { !it.paid } ?: return
        pay(method, part.amountMinor, part.amountMinor)
        split.value = current.copy(
            parts = current.parts.mapIndexed { i, p -> if (i == index) p.copy(paid = true) else p },
        )
    }

    /** Recomputes the parts from the current mode; parts always sum to the order total. */
    private fun recomputed(state: SplitUiState): SplitUiState {
        val total = Money(uiState.value.totalMinor)
        val lines = uiState.value.lines
        val parts = when (state.mode) {
            SplitMode.EVENLY -> BillSplit.evenly(total, state.ways)

            SplitMode.BY_ITEM -> {
                // An unassigned line is shared, so its value is spread evenly across guests.
                val perGuest = LongArray(state.ways)
                lines.forEach { line ->
                    val guest = state.assignment[line.id]
                    if (guest != null) {
                        perGuest[guest] += line.lineTotalMinor
                    } else {
                        Money(line.lineTotalMinor).splitEvenly(state.ways)
                            .forEachIndexed { i, share -> perGuest[i] += share.minor }
                    }
                }
                val weights = (0 until state.ways).associate { "Guest ${it + 1}" to Money(perGuest[it]) }
                if (weights.values.all { it.minor <= 0L }) {
                    BillSplit.evenly(total, state.ways)
                } else {
                    BillSplit.byItem(total, weights)
                }
            }
        }
        return state.copy(parts = parts.map { SplitPartUi(it.label, it.amount.minor) })
    }

    private inline fun MutableStateFlow<SplitUiState?>.update(transform: (SplitUiState) -> SplitUiState) {
        value = value?.let(transform)
    }

    companion object {
        const val MIN_WAYS = 2
        const val MAX_WAYS = 8
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
