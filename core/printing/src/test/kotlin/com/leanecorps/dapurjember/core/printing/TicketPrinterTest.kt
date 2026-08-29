package com.leanecorps.dapurjember.core.printing

import com.leanecorps.dapurjember.core.domain.printing.KitchenTicketData
import com.leanecorps.dapurjember.core.domain.printing.KitchenTicketLine
import com.leanecorps.dapurjember.core.domain.printing.PrintJobType
import com.leanecorps.dapurjember.core.domain.printing.ReceiptData
import com.leanecorps.dapurjember.core.domain.printing.TicketPrinter
import com.leanecorps.dapurjember.core.printing.escpos.EscPosDecoder
import com.leanecorps.dapurjember.core.testing.repository.FakePrintQueue
import com.leanecorps.dapurjember.core.testing.repository.FakePrintQueueScheduler
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TicketPrinterTest {

    private val queue = FakePrintQueue()
    private val scheduler = FakePrintQueueScheduler()
    private val printer = TicketPrinter(DefaultTicketRenderer(), queue, scheduler)

    private fun kitchenTicket(lines: List<KitchenTicketLine>, reprint: Boolean = false) = KitchenTicketData(
        storeName = "Dapur Jember",
        stationTitle = "Kitchen",
        orderNumber = "A-1",
        tableLabel = "5",
        orderType = "Dine-in",
        serverName = "Wira",
        printedAt = "2026-08-29 19:00",
        lines = lines,
        reprint = reprint,
    )

    @Test
    fun `kitchen ticket with unsent lines is enqueued as ESC-POS bytes`() = runTest {
        val jobId = printer.printKitchenTicket(
            kitchenTicket(listOf(KitchenTicketLine(1, "Indomie"))),
            paperWidthMm = 80,
        )

        assertEquals(1, queue.enqueued.size)
        val job = queue.enqueued.single()
        assertEquals(jobId, job.id)
        assertEquals(PrintJobType.KITCHEN, job.type)
        assertTrue(EscPosDecoder.text(job.payload).contains("1 x Indomie"))
        assertEquals(1, scheduler.drainSoonCalls)
    }

    @Test
    fun `an empty non-reprint kitchen ticket is never enqueued`() = runTest {
        val jobId = printer.printKitchenTicket(kitchenTicket(emptyList()), paperWidthMm = 80)

        assertNull(jobId)
        assertTrue(queue.enqueued.isEmpty())
        assertEquals(0, scheduler.drainSoonCalls)
    }

    @Test
    fun `an empty reprint kitchen ticket is still enqueued`() = runTest {
        printer.printKitchenTicket(kitchenTicket(emptyList(), reprint = true), paperWidthMm = 58)

        assertEquals(1, queue.enqueued.size)
        assertTrue(EscPosDecoder.text(queue.enqueued.single().payload).contains("REPRINT"))
    }

    @Test
    fun `receipt is enqueued as a RECEIPT job`() = runTest {
        val receipt = ReceiptData(
            headerLines = listOf("Dapur Jember"),
            orderNumber = "A-1",
            businessDay = "2026-08-29",
            tableLabel = "5",
            printedAt = "2026-08-29 20:00",
            serverName = "Sari",
            lines = emptyList(),
            subtotalMinor = 10_000,
            discountMinor = 0,
            serviceChargeMinor = 0,
            taxMinor = 0,
            roundingMinor = 0,
            totalMinor = 10_000,
            payments = emptyList(),
            changeMinor = 0,
            currencyCode = "IDR",
            currencyMinorUnits = 0,
            footerLines = listOf("Terima kasih"),
        )

        printer.printReceipt(receipt, paperWidthMm = 80)

        assertEquals(PrintJobType.RECEIPT, queue.enqueued.single().type)
    }
}
