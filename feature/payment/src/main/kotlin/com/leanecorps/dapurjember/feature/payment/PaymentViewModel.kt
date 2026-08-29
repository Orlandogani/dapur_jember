package com.leanecorps.dapurjember.feature.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.domain.order.OrderRepository
import com.leanecorps.dapurjember.core.domain.order.OrderState
import com.leanecorps.dapurjember.core.domain.order.PaymentMethod
import com.leanecorps.dapurjember.core.domain.order.RecordPaymentParams
import com.leanecorps.dapurjember.core.domain.order.SettleOrderUseCase
import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
) {
    val balanceMinor: Long get() = (totalMinor - paidMinor).coerceAtLeast(0)
}

data class PaidLineUi(val method: PaymentMethod, val amountMinor: Long, val changeMinor: Long)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
    private val sessionRepository: SessionRepository,
    private val settleOrder: SettleOrderUseCase,
) : ViewModel() {

    private val orderId: String = requireNotNull(savedStateHandle[PAYMENT_ORDER_ID_ARG])

    val uiState: StateFlow<PaymentUiState> = orderRepository.observeOrder(orderId)
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
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PaymentUiState())

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

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
