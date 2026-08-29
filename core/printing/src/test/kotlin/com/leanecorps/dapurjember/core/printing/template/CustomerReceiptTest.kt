package com.leanecorps.dapurjember.core.printing.template

import com.leanecorps.dapurjember.core.printing.PaperWidth
import com.leanecorps.dapurjember.core.printing.escpos.EscPosDecoder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CustomerReceiptTest {

    private fun receipt(
        discountMinor: Long = 0,
        serviceChargeMinor: Long = 0,
        taxMinor: Long = 0,
        roundingMinor: Long = 0,
        changeMinor: Long = 0,
    ) = ReceiptData(
        headerLines = listOf("Dapur Jember", "Jl. Trunojoyo 12", "Jember"),
        orderNumber = "A-014",
        businessDay = "2026-08-29",
        tableLabel = "12",
        printedAt = "2026-08-29 20:05",
        serverName = "Sari",
        lines = listOf(
            ReceiptItemLine(
                quantity = 2,
                name = "Nasi Goreng Ayam",
                lineTotalMinor = 40_000,
                modifiers = listOf(ReceiptModifierLine("Extra cheese", 5_000)),
            ),
            ReceiptItemLine(quantity = 1, name = "Es Teh", lineTotalMinor = 5_000),
        ),
        subtotalMinor = 45_000,
        discountMinor = discountMinor,
        serviceChargeMinor = serviceChargeMinor,
        taxMinor = taxMinor,
        roundingMinor = roundingMinor,
        totalMinor = 45_000 - discountMinor + serviceChargeMinor + taxMinor + roundingMinor,
        payments = listOf(ReceiptPaymentLine("CASH", 50_000)),
        changeMinor = changeMinor,
        currencyCode = "IDR",
        currencyMinorUnits = 0,
        footerLines = listOf("Terima kasih"),
    )

    @Test
    fun `renders items, modifiers and the grouped total`() {
        val text = EscPosDecoder.text(renderCustomerReceipt(receipt(), PaperWidth.MM_80))
        assertTrue(text.contains("Dapur Jember"))
        assertTrue(text.contains("2 x Nasi Goreng Ayam"))
        assertTrue(text.contains("40,000"))
        assertTrue(text.contains("+ Extra cheese"))
        assertTrue(text.contains("Subtotal"))
        assertTrue(text.contains("TOTAL"))
        assertTrue(text.contains("IDR 45,000"))
        assertTrue(text.contains("CASH"))
        assertTrue(text.contains("Terima kasih"))
    }

    @Test
    fun `optional total rows appear only when non-zero`() {
        val plain = EscPosDecoder.text(renderCustomerReceipt(receipt(), PaperWidth.MM_80))
        assertFalse(plain.contains("Discount"))
        assertFalse(plain.contains("Tax"))
        assertFalse(plain.contains("Change"))

        val full = EscPosDecoder.text(
            renderCustomerReceipt(
                receipt(discountMinor = 5_000, serviceChargeMinor = 2_000, taxMinor = 4_200, changeMinor = 5_000),
                PaperWidth.MM_80,
            ),
        )
        assertTrue(full.contains("Discount"))
        assertTrue(full.contains("-5,000"))
        assertTrue(full.contains("Service charge"))
        assertTrue(full.contains("Tax"))
        assertTrue(full.contains("Change"))
    }

    @Test
    fun `every printed line fits the 58mm width`() {
        val lines = EscPosDecoder.lines(renderCustomerReceipt(receipt(taxMinor = 4_200), PaperWidth.MM_58))
        assertTrue(lines.all { it.length <= PaperWidth.MM_58.columns }, "overflow: ${lines.filter { it.length > 32 }}")
    }
}
