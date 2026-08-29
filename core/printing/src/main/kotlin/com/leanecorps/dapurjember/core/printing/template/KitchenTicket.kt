package com.leanecorps.dapurjember.core.printing.template

import com.leanecorps.dapurjember.core.domain.printing.KitchenTicketData
import com.leanecorps.dapurjember.core.domain.printing.KitchenTicketLine
import com.leanecorps.dapurjember.core.printing.PaperWidth
import com.leanecorps.dapurjember.core.printing.escpos.Alignment
import com.leanecorps.dapurjember.core.printing.escpos.EscPosBuilder

/**
 * Renders [data] to ESC/POS bytes for [width]. Big type, lots of whitespace — a cook reads
 * this across a hot line in a hurry. An empty line list renders an explicit marker rather
 * than a blank ticket.
 */
fun renderKitchenTicket(data: KitchenTicketData, width: PaperWidth): ByteArray {
    val b = EscPosBuilder(width.columns)
    b.initialize()

    b.align(Alignment.CENTER)
    b.size(1, 2)
    b.bold(true)
    b.line(data.stationTitle.uppercase())
    b.bold(false)
    b.normalSize()
    b.line(data.storeName)
    if (data.reprint) b.line("*** REPRINT ***")
    b.align(Alignment.LEFT)
    b.divider()

    b.size(1, 2)
    b.bold(true)
    b.line(data.tableLabel?.let { "TABLE $it" } ?: data.orderType.uppercase())
    b.bold(false)
    b.normalSize()
    b.row("Order", data.orderNumber)
    b.row("Type", data.orderType)
    b.row("Server", data.serverName)
    b.row("Time", data.printedAt)
    b.divider()

    if (data.lines.isEmpty()) {
        b.line("(no new items)")
    } else {
        renderLinesByCourse(b, data.lines)
    }

    b.divider()
    b.feed(1)
    b.cut()
    return b.build()
}

private fun renderLinesByCourse(b: EscPosBuilder, lines: List<KitchenTicketLine>) {
    val byCourse = lines.groupBy { it.course }.toSortedMap()
    val multipleCourses = byCourse.size > 1
    byCourse.forEach { (course, courseLines) ->
        if (multipleCourses) {
            b.bold(true)
            b.line("-- Course $course --")
            b.bold(false)
        }
        courseLines.forEach { renderLine(b, it) }
    }
}

private fun renderLine(b: EscPosBuilder, line: KitchenTicketLine) {
    b.size(1, 2)
    b.bold(true)
    b.line("${line.quantity} x ${line.name}")
    b.bold(false)
    b.normalSize()
    line.modifiers.forEach { b.line("   + $it") }
    line.note?.takeIf { it.isNotBlank() }?.let { b.line("   ! $it") }
}
