package com.leanecorps.dapurjember.core.domain.printing

import javax.inject.Inject

/**
 * Renders a ticket and drops it on the [PrintQueue]. Call this from inside the same
 * transaction as the mutation that triggered the print (send-to-kitchen, settle) so the job
 * cannot be lost between sale-commit and enqueue. The queue drainer does the actual printing
 * off that thread; a dead printer never blocks the caller (FR-PR3).
 */
class TicketPrinter @Inject constructor(
    private val renderer: TicketRenderer,
    private val queue: PrintQueue,
) {

    /**
     * Enqueues a kitchen ticket for the lines in [ticket]. Returns the job id, or `null` when
     * there is nothing to print (no unsent lines and not a reprint) — never queues a blank
     * ticket (flows doc §3: "visibly impossible to double-print").
     */
    suspend fun printKitchenTicket(
        ticket: KitchenTicketData,
        paperWidthMm: Int,
        targetPrinterId: String? = null,
    ): String? {
        if (ticket.lines.isEmpty() && !ticket.reprint) return null
        val payload = renderer.renderKitchenTicket(ticket, paperWidthMm)
        return queue.enqueue(PrintJobType.KITCHEN, payload, targetPrinterId)
    }

    suspend fun printReceipt(
        receipt: ReceiptData,
        paperWidthMm: Int,
        targetPrinterId: String? = null,
    ): String {
        val payload = renderer.renderReceipt(receipt, paperWidthMm)
        return queue.enqueue(PrintJobType.RECEIPT, payload, targetPrinterId)
    }
}
