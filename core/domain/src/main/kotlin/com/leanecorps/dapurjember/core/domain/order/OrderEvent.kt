package com.leanecorps.dapurjember.core.domain.order

/** An action that may advance an order's [OrderState]. */
enum class OrderEvent {
    /** Send the order (or its unsent lines) to the kitchen. */
    SEND,

    /** Some but not all lines have been served. */
    SERVE_PARTIAL,

    /** All lines have been served. */
    SERVE,

    /** The bill has been settled in full. */
    PAY,

    /** Close the paid order and free the table. */
    CLOSE,

    /** Void the whole order (audit-logged). Not a refund path. */
    VOID,
}
