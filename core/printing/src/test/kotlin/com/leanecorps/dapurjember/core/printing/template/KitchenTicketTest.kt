package com.leanecorps.dapurjember.core.printing.template

import com.leanecorps.dapurjember.core.printing.PaperWidth
import com.leanecorps.dapurjember.core.printing.escpos.EscPos
import com.leanecorps.dapurjember.core.printing.escpos.EscPosDecoder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KitchenTicketTest {

    private fun ticket(
        lines: List<KitchenTicketLine>,
        tableLabel: String? = "12",
        reprint: Boolean = false,
    ) = KitchenTicketData(
        storeName = "Dapur Jember",
        stationTitle = "Kitchen",
        orderNumber = "A-014",
        tableLabel = tableLabel,
        orderType = "Dine-in",
        serverName = "Wira",
        printedAt = "2026-08-29 19:42",
        lines = lines,
        reprint = reprint,
    )

    @Test
    fun `renders header, table and every line`() {
        val job = renderKitchenTicket(
            ticket(
                listOf(
                    KitchenTicketLine(2, "Nasi Goreng Ayam", modifiers = listOf("No onion"), note = "extra spicy"),
                    KitchenTicketLine(1, "Es Teh"),
                ),
            ),
            PaperWidth.MM_80,
        )
        val text = EscPosDecoder.text(job)

        assertTrue(text.contains("KITCHEN"))
        assertTrue(text.contains("Dapur Jember"))
        assertTrue(text.contains("TABLE 12"))
        assertTrue(text.contains("A-014"))
        assertTrue(text.contains("2 x Nasi Goreng Ayam"))
        assertTrue(text.contains("   + No onion"))
        assertTrue(text.contains("   ! extra spicy"))
        assertTrue(text.contains("1 x Es Teh"))
    }

    @Test
    fun `starts with initialize and ends with a cut`() {
        val job = renderKitchenTicket(ticket(listOf(KitchenTicketLine(1, "Indomie"))), PaperWidth.MM_58)
        assertEquals(EscPos.ESC, job[0])
        assertEquals('@'.code.toByte(), job[1])
        val tail = job.copyOfRange(job.size - 4, job.size).toList()
        assertEquals(listOf(EscPos.GS, 'V'.code.toByte(), 66.toByte(), 3.toByte()), tail)
    }

    @Test
    fun `groups lines by course only when there is more than one`() {
        val single = EscPosDecoder.text(
            renderKitchenTicket(
                ticket(listOf(KitchenTicketLine(1, "Soup", course = 1), KitchenTicketLine(1, "Rice", course = 1))),
                PaperWidth.MM_80,
            ),
        )
        assertFalse(single.contains("Course"))

        val multi = EscPosDecoder.text(
            renderKitchenTicket(
                ticket(listOf(KitchenTicketLine(1, "Soup", course = 1), KitchenTicketLine(1, "Steak", course = 2))),
                PaperWidth.MM_80,
            ),
        )
        assertTrue(multi.contains("-- Course 1 --"))
        assertTrue(multi.contains("-- Course 2 --"))
    }

    @Test
    fun `takeaway prints the order type instead of a table`() {
        val text = EscPosDecoder.text(
            renderKitchenTicket(
                ticket(listOf(KitchenTicketLine(1, "Indomie")), tableLabel = null).copy(orderType = "Takeaway"),
                PaperWidth.MM_80,
            ),
        )
        assertFalse(text.contains("TABLE"))
        assertTrue(text.contains("TAKEAWAY"))
    }

    @Test
    fun `an empty line list renders an explicit marker, never a blank ticket`() {
        val text = EscPosDecoder.text(renderKitchenTicket(ticket(emptyList()), PaperWidth.MM_80))
        assertTrue(text.contains("(no new items)"))
    }

    @Test
    fun `reprint is flagged`() {
        val text = EscPosDecoder.text(
            renderKitchenTicket(ticket(listOf(KitchenTicketLine(1, "Indomie")), reprint = true), PaperWidth.MM_80),
        )
        assertTrue(text.contains("*** REPRINT ***"))
    }
}
