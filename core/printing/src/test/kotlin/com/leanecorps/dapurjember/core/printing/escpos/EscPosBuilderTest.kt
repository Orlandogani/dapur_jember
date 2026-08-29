package com.leanecorps.dapurjember.core.printing.escpos

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EscPosBuilderTest {

    @Test
    fun `initialize is emitted verbatim at the head`() {
        val job = EscPosBuilder(32).initialize().build()
        assertEquals(EscPos.ESC, job[0])
        assertEquals('@'.code.toByte(), job[1])
    }

    @Test
    fun `line word-wraps to the column count`() {
        val job = EscPosBuilder(16).line("the quick brown fox jumps").build()
        assertEquals(listOf("the quick brown", "fox jumps", ""), EscPosDecoder.lines(job))
    }

    @Test
    fun `line breaks a word longer than the width`() {
        val job = EscPosBuilder(8).line("supercalifragilistic").build()
        assertEquals(listOf("supercal", "ifragili", "stic", ""), EscPosDecoder.lines(job))
    }

    @Test
    fun `row pads left and right to the full width`() {
        val job = EscPosBuilder(20).row("Subtotal", "15,000").build()
        val rendered = EscPosDecoder.text(job).trimEnd('\n')
        assertEquals(20, rendered.length)
        assertTrue(rendered.startsWith("Subtotal"))
        assertTrue(rendered.endsWith("15,000"))
        assertEquals("Subtotal${" ".repeat(6)}15,000", rendered)
    }

    @Test
    fun `row wraps the right side onto its own line when it cannot fit`() {
        val job = EscPosBuilder(12).row("A very long label", "9,999").build()
        val lines = EscPosDecoder.lines(job).dropLast(1)
        assertEquals("9,999", lines.last().trimStart())
        assertTrue(lines.last().length == 12)
    }

    @Test
    fun `divider fills exactly one line`() {
        val job = EscPosBuilder(10).divider().build()
        assertEquals(listOf("----------", ""), EscPosDecoder.lines(job))
    }

    @Test
    fun `cut appends the partial-cut sequence last`() {
        val job = EscPosBuilder(32).line("x").cut().build()
        val tail = job.copyOfRange(job.size - 4, job.size)
        assertEquals(listOf(EscPos.GS, 'V'.code.toByte(), 66.toByte(), 3.toByte()), tail.toList())
    }

    @Test
    fun `openDrawer emits the ESC p pulse`() {
        val job = EscPosBuilder(32).openDrawer().build()
        assertEquals(EscPos.ESC, job[0])
        assertEquals('p'.code.toByte(), job[1])
        assertEquals(0.toByte(), job[2])
    }
}
