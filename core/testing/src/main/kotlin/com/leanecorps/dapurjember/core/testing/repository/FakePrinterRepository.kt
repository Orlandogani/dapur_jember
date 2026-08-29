package com.leanecorps.dapurjember.core.testing.repository

import com.leanecorps.dapurjember.core.domain.printing.Printer
import com.leanecorps.dapurjember.core.domain.printing.PrinterRepository
import com.leanecorps.dapurjember.core.domain.printing.PrinterRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakePrinterRepository(initial: List<Printer> = emptyList()) : PrinterRepository {

    private val printers = MutableStateFlow(initial)

    override fun observePrinters(): Flow<List<Printer>> = printers

    override suspend fun getPrinter(id: String): Printer? = printers.value.firstOrNull { it.id == id }

    override suspend fun printersForRole(role: PrinterRole): List<Printer> =
        printers.value.filter { role in it.roles }

    override suspend fun savePrinter(printer: Printer) = printers.update { list ->
        list.filterNot { it.id == printer.id } + printer
    }

    override suspend fun removePrinter(id: String) = printers.update { list -> list.filterNot { it.id == id } }
}
