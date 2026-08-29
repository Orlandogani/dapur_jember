package com.leanecorps.dapurjember.core.printing

import com.leanecorps.dapurjember.core.domain.printing.PrintJobState
import com.leanecorps.dapurjember.core.domain.printing.PrintJobType
import com.leanecorps.dapurjember.core.domain.printing.Printer
import com.leanecorps.dapurjember.core.domain.printing.PrinterLink
import com.leanecorps.dapurjember.core.domain.printing.PrinterRole
import com.leanecorps.dapurjember.core.printing.transport.PrinterTransport
import com.leanecorps.dapurjember.core.printing.transport.PrinterTransportException
import com.leanecorps.dapurjember.core.printing.transport.PrinterTransportFactory
import com.leanecorps.dapurjember.core.testing.repository.FakePrintQueue
import com.leanecorps.dapurjember.core.testing.repository.FakePrinterRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrintDispatcherTest {

    private val queue = FakePrintQueue()
    private val sent = mutableListOf<ByteArray>()

    private var failWith: String? = null
    private val transports = object : PrinterTransportFactory {
        override fun create(printer: Printer) = create(printer.link, printer.address)
        override fun create(link: PrinterLink, address: String) = PrinterTransport { bytes ->
            failWith?.let { throw PrinterTransportException(it) }
            sent += bytes
        }
    }

    private fun dispatcher(printers: List<Printer>) =
        PrintDispatcher(queue, FakePrinterRepository(printers), transports)

    private val kitchenPrinter = Printer(
        id = "p1",
        name = "Kitchen",
        link = PrinterLink.TCP,
        address = "10.0.0.5:9100",
        paperWidthMm = 80,
        codepage = 0,
        roles = setOf(PrinterRole.KITCHEN),
    )

    @Test
    fun `a job is sent to the printer for its role and marked DONE`() = runTest {
        queue.enqueue(PrintJobType.KITCHEN, byteArrayOf(1, 2, 3))

        val outcome = dispatcher(listOf(kitchenPrinter)).drainOnce()

        assertEquals(1, outcome.printed)
        assertFalse(outcome.retryNeeded)
        assertEquals(listOf(listOf<Byte>(1, 2, 3)), sent.map { it.toList() })
        assertEquals(PrintJobState.DONE, queue.observeJobs().first().single().state)
    }

    @Test
    fun `with no printer configured the job stays PENDING and a retry is requested`() = runTest {
        queue.enqueue(PrintJobType.KITCHEN, byteArrayOf(1))

        val outcome = dispatcher(emptyList()).drainOnce()

        assertEquals(0, outcome.printed)
        assertTrue(outcome.retryNeeded)
        assertTrue(sent.isEmpty())
        assertEquals(PrintJobState.PENDING, queue.observeJobs().first().single().state)
    }

    @Test
    fun `a transport failure marks the job FAILED with the error and requests a retry`() = runTest {
        queue.enqueue(PrintJobType.KITCHEN, byteArrayOf(1))
        failWith = "connection refused"

        val outcome = dispatcher(listOf(kitchenPrinter)).drainOnce()

        assertEquals(1, outcome.failed)
        assertTrue(outcome.retryNeeded)
        val job = queue.observeJobs().first().single()
        assertEquals(PrintJobState.FAILED, job.state)
        assertEquals("connection refused", job.lastError)
        assertEquals(1, job.attempts)
    }

    @Test
    fun `a RECEIPT job routes to a RECEIPT-role printer`() = runTest {
        val receiptPrinter = kitchenPrinter.copy(id = "p2", roles = setOf(PrinterRole.RECEIPT))
        queue.enqueue(PrintJobType.RECEIPT, byteArrayOf(7))

        dispatcher(listOf(receiptPrinter)).drainOnce()

        assertEquals(listOf(listOf<Byte>(7)), sent.map { it.toList() })
    }
}
