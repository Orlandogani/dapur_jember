package com.leanecorps.dapurjember.core.printing.escpos

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

enum class Alignment(internal val code: Int) { LEFT(0), CENTER(1), RIGHT(2) }

/**
 * Fluent assembler for an ESC/POS job. Text methods are column-aware so one template renders
 * correctly on both 58 mm (32 col) and 80 mm (48 col) paper (architecture §6 — templates are
 * width-parameterised, never hard-coded).
 *
 * Not thread-safe; build one per job.
 */
class EscPosBuilder(
    private val columns: Int,
    private val charset: Charset = Charsets.ISO_8859_1,
) {
    private val out = ByteArrayOutputStream()

    fun initialize() = raw(EscPos.INITIALIZE)

    fun align(alignment: Alignment) = raw(EscPos.align(alignment.code))

    fun bold(on: Boolean) = raw(EscPos.bold(on))

    fun underline(on: Boolean) = raw(EscPos.underline(if (on) 1 else 0))

    /** Character magnification, 1..8 in each axis. */
    fun size(widthMultiplier: Int, heightMultiplier: Int) =
        raw(EscPos.size(widthMultiplier, heightMultiplier))

    fun normalSize() = size(1, 1)

    fun codepage(table: Int) = raw(EscPos.codepage(table))

    /** Raw text, no line break, no wrapping. */
    fun text(value: String) = raw(value.toByteArray(charset))

    /**
     * One or more lines. A string that already fits on one line is emitted verbatim
     * (leading indentation preserved); anything longer is word-wrapped to [columns].
     */
    fun line(value: String = ""): EscPosBuilder {
        val rows = when {
            value.isEmpty() -> listOf("")
            '\n' !in value && value.length <= columns -> listOf(value)
            else -> wrapText(value, columns)
        }
        rows.forEach { text(it).newline() }
        return this
    }

    fun boldLine(value: String): EscPosBuilder {
        bold(true)
        line(value)
        return bold(false)
    }

    fun centerLine(value: String): EscPosBuilder {
        align(Alignment.CENTER)
        line(value)
        return align(Alignment.LEFT)
    }

    /**
     * [left] flush left, [right] flush right, on one line of [columns]. If they cannot both
     * fit (with at least one space between), [right] drops to its own right-aligned line.
     */
    fun row(left: String, right: String): EscPosBuilder {
        if (left.length + right.length + 1 <= columns) {
            val gap = columns - left.length - right.length
            return text(left + " ".repeat(gap) + right).newline()
        }
        wrapText(left, columns).forEach { text(it).newline() }
        val pad = (columns - right.length).coerceAtLeast(0)
        return text(" ".repeat(pad) + right).newline()
    }

    fun divider(fill: Char = '-'): EscPosBuilder = text(fill.toString().repeat(columns)).newline()

    fun feed(lines: Int = 1) = raw(EscPos.feed(lines))

    fun cut() = raw(EscPos.cut())

    fun openDrawer() = raw(EscPos.openDrawer())

    fun newline() = raw(byteArrayOf(EscPos.LF))

    fun raw(bytes: ByteArray): EscPosBuilder {
        out.write(bytes)
        return this
    }

    fun build(): ByteArray = out.toByteArray()
}
