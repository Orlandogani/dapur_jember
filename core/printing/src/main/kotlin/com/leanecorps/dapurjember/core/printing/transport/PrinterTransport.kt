package com.leanecorps.dapurjember.core.printing.transport

import com.leanecorps.dapurjember.core.domain.printing.Printer
import com.leanecorps.dapurjember.core.domain.printing.PrinterLink

/**
 * A one-shot channel to a physical printer. [send] opens the link, writes every byte, flushes,
 * and closes — throwing [PrinterTransportException] on any failure so the [PrintDispatcher]
 * can mark the job for retry. Implementations are stateless; one is created per attempt.
 */
fun interface PrinterTransport {
    suspend fun send(bytes: ByteArray)
}

class PrinterTransportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Builds the right [PrinterTransport] for a printer's [PrinterLink]. */
interface PrinterTransportFactory {
    fun create(printer: Printer): PrinterTransport

    fun create(link: PrinterLink, address: String): PrinterTransport
}
