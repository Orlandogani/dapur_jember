package com.leanecorps.dapurjember.core.printing.escpos

/**
 * Raw ESC/POS control sequences. Vendor-independent — see architecture §6. Kept as a flat
 * table of byte arrays so [EscPosBuilder] stays a thin assembler and every sequence is
 * greppable against a printer manual.
 */
internal object EscPos {
    const val ESC: Byte = 0x1B
    const val GS: Byte = 0x1D
    const val LF: Byte = 0x0A

    /** ESC @ — reset to power-on defaults. */
    val INITIALIZE = byteArrayOf(ESC, '@'.code.toByte())

    /** ESC a n — 0 left, 1 centre, 2 right. */
    fun align(n: Int) = byteArrayOf(ESC, 'a'.code.toByte(), n.toByte())

    /** ESC E n — emphasis (bold) on/off. */
    fun bold(on: Boolean) = byteArrayOf(ESC, 'E'.code.toByte(), if (on) 1 else 0)

    /** ESC - n — underline off/1-dot/2-dot. */
    fun underline(n: Int) = byteArrayOf(ESC, '-'.code.toByte(), n.toByte())

    /**
     * GS ! n — character size. Low nibble = width multiplier - 1, high nibble = height
     * multiplier - 1 (each 1..8).
     */
    fun size(widthMul: Int, heightMul: Int): ByteArray {
        val w = (widthMul - 1).coerceIn(0, 7)
        val h = (heightMul - 1).coerceIn(0, 7)
        return byteArrayOf(GS, '!'.code.toByte(), ((w shl 4) or h).toByte())
    }

    /** ESC t n — select character code table (codepage). */
    fun codepage(n: Int) = byteArrayOf(ESC, 't'.code.toByte(), n.toByte())

    /** ESC d n — feed n lines. */
    fun feed(lines: Int) = byteArrayOf(ESC, 'd'.code.toByte(), lines.coerceIn(0, 255).toByte())

    /** GS V 66 n — feed n and partial cut. */
    fun cut(feedBefore: Int = 3) =
        byteArrayOf(GS, 'V'.code.toByte(), 66, feedBefore.coerceIn(0, 255).toByte())

    /**
     * ESC p m t1 t2 — pulse the drawer-kick connector on pin [pin] (0 → pin 2, 1 → pin 5).
     * The cash drawer is wired to the printer, not to Android (architecture §9).
     */
    fun openDrawer(pin: Int = 0) =
        byteArrayOf(ESC, 'p'.code.toByte(), pin.toByte(), 25, (250 / 2).toByte())
}
