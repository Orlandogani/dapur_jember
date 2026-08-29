package com.leanecorps.dapurjember.core.domain.printing

import kotlinx.coroutines.flow.Flow

/** What a configured printer prints (FR-PR2). One device may hold several roles. */
enum class PrinterRole { KITCHEN, BAR, RECEIPT }

/** Physical link to a printer (architecture §6). */
enum class PrinterLink { BLUETOOTH, USB, TCP }

/**
 * A configured printer. [address] is a Bluetooth MAC, a USB device name, or `host:port` for
 * TCP. Device-local — a printer paired to this tablet is not reachable from a peer.
 */
data class Printer(
    val id: String,
    val name: String,
    val link: PrinterLink,
    val address: String,
    val paperWidthMm: Int,
    val codepage: Int,
    val roles: Set<PrinterRole>,
)

interface PrinterRepository {
    fun observePrinters(): Flow<List<Printer>>

    suspend fun getPrinter(id: String): Printer?

    suspend fun printersForRole(role: PrinterRole): List<Printer>

    suspend fun savePrinter(printer: Printer)

    suspend fun removePrinter(id: String)
}
