package com.leanecorps.dapurjember.core.domain.order

/**
 * The lifecycle state of an order (`docs/2-architecture` 5.1, FR-O5). Persisted verbatim as
 * `orders.state`. Transitions are governed by [OrderStateMachine] — never mutate this
 * directly from the UI.
 */
enum class OrderState {
    DRAFT,
    SENT,
    PARTIALLY_SERVED,
    SERVED,
    PAID,
    CLOSED,
    VOIDED,
    ;

    /** No transition leaves a terminal state. */
    val isTerminal: Boolean get() = this == CLOSED || this == VOIDED

    /** The string stored in `orders.state`. */
    val storageValue: String get() = name

    companion object {
        /** Parses a stored `orders.state` value, failing loudly on anything unrecognised. */
        fun fromStorage(value: String): OrderState =
            entries.firstOrNull { it.name == value }
                ?: throw IllegalArgumentException("unknown order state: '$value'")
    }
}
