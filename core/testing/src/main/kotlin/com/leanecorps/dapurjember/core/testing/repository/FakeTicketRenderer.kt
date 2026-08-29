package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.domain.printing.KitchenTicketData
import com.leanecorps.dapurjember.core.domain.printing.ReceiptData
import com.leanecorps.dapurjember.core.domain.printing.TicketRenderer

/** Records what it was asked to render and returns a marker payload. */
class FakeTicketRenderer : TicketRenderer {

    val kitchenTickets = mutableListOf<KitchenTicketData>()
    val receipts = mutableListOf<ReceiptData>()

    override fun renderKitchenTicket(data: KitchenTicketData, paperWidthMm: Int): ByteArray {
        kitchenTickets += data
        return "KITCHEN:${data.orderNumber}".toByteArray()
    }

    override fun renderReceipt(data: ReceiptData, paperWidthMm: Int): ByteArray {
        receipts += data
        return "RECEIPT:${data.orderNumber}".toByteArray()
    }
}
