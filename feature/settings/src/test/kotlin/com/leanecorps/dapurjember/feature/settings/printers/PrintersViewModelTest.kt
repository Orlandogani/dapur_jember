package com.leanecorps.dapurjember.feature.settings.printers

import com.leanecorps.dapurjember.core.domain.printing.PrinterLink
import com.leanecorps.dapurjember.core.domain.printing.PrinterRole
import com.leanecorps.dapurjember.core.domain.printing.TicketPrinter
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakePrintQueue
import com.leanecorps.dapurjember.core.testing.repository.FakePrintQueueScheduler
import com.leanecorps.dapurjember.core.testing.repository.FakePrinterRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeTicketRenderer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class PrintersViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val printers = FakePrinterRepository()
    private val queue = FakePrintQueue()
    private val viewModel by lazy {
        PrintersViewModel(
            printers = printers,
            ticketPrinter = TicketPrinter(FakeTicketRenderer(), queue, FakePrintQueueScheduler()),
        )
    }

    @Test
    fun `adding and saving a printer persists it with the chosen roles`() = runTest {
        viewModel.startAdd()
        viewModel.edit { it.copy(name = "Front counter", address = "10.0.0.5:9100") }
        viewModel.toggleRole(PrinterRole.KITCHEN)
        viewModel.save()
        advanceUntilIdle()

        val saved = printers.printersForRole(PrinterRole.RECEIPT).single()
        assertEquals("Front counter", saved.name)
        assertEquals(setOf(PrinterRole.RECEIPT, PrinterRole.KITCHEN), saved.roles)
    }

    @Test
    fun `save is a no-op while the draft is incomplete`() = runTest {
        viewModel.startAdd()
        viewModel.edit { it.copy(name = "No address") }
        viewModel.save()
        advanceUntilIdle()

        assertTrue(printers.printersForRole(PrinterRole.RECEIPT).isEmpty())
    }

    @Test
    fun `test print queues a job aimed at that printer`() = runTest {
        viewModel.startAdd()
        viewModel.edit { it.copy(name = "Kitchen", link = PrinterLink.TCP, address = "10.0.0.9:9100") }
        viewModel.save()
        advanceUntilIdle()
        val id = printers.printersForRole(PrinterRole.RECEIPT).single().id

        viewModel.testPrint(id)
        advanceUntilIdle()

        val job = queue.enqueued.single()
        assertEquals(id, job.targetPrinterId)
        assertEquals("KITCHEN", job.type.name)
    }
}
