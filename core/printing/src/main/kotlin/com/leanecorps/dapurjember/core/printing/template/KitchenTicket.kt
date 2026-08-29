package com.leanecorps.dapurjember.core.printing.template

import com.leanecorps.dapurjember.core.printing.PaperWidth
import com.leanecorps.dapurjember.core.printing.escpos.Alignment
import com.leanecorps.dapurjember.core.printing.escpos.EscPosBuilder

/**
 * Input for a kitchen (or bar) ticket. Caller resolves ids to names and pre-formats the
 * timestamp in the store timezone — the template does no lookups and no time maths.
 *
 * [lines] is already filtered to the lines this ticket should show (FR-O3: only lines not
 * previously sent). An empty list is a caller bug and renders an explicit marker.
 */
data class KitchenTicketData(
    val storeName: String,
    val stationTitle: String,
    val orderNumber: String,
    val tableLabel: String?,
    val orderType: String,
    val serverName: String,
    val printedAt: String,
    val lines: List<KitchenTicketLine>,
    val reprint: Boolean = false,
)

data class KitchenTicketLine(
    val quantity: Int,
    val name: String,
    val modifiers: List<String> = emptyList(),
    val note: String? = null,
    val course: Int = 1,
)

/**
 * Renders [data] to ESC/POS bytes for [width]. Big type, lots of whitespace — a cook reads
 * this across a hot line in a hurry.
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
