package com.leanecorps.dapurjember.core.domain.printing

/**
 * Turns ticket data into ESC/POS bytes. The implementation lives in `:core:printing`; the
 * domain and data layers depend only on this interface (layering: `feature` → `core:domain`
 * ← `core:data` / `core:printing`).
 */
interface TicketRenderer {
    fun renderKitchenTicket(data: KitchenTicketData, paperWidthMm: Int): ByteArray

    fun renderReceipt(data: ReceiptData, paperWidthMm: Int): ByteArray
}
