package com.leanecorps.dapurjember.feature.settings.printers

import com.leanecorps.dapurjember.core.domain.printing.Printer
import com.leanecorps.dapurjember.core.domain.printing.PrinterLink
import com.leanecorps.dapurjember.core.domain.printing.PrinterRole

data class PrintersUiState(
    val loading: Boolean = true,
    val printers: List<PrinterRowUi> = emptyList(),
    val editor: PrinterEditorUi? = null,
    val message: String? = null,
)

data class PrinterRowUi(
    val id: String,
    val name: String,
    val transport: String,
    val address: String,
    val roles: String,
)

data class PrinterEditorUi(
    val id: String? = null,
    val name: String = "",
    val link: PrinterLink = PrinterLink.TCP,
    val address: String = "",
    val paperWidthMm: Int = 80,
    val codepageText: String = "0",
    val roles: Set<PrinterRole> = setOf(PrinterRole.RECEIPT),
) {
    val codepage: Int? get() = codepageText.trim().toIntOrNull()
    val isNew: Boolean get() = id == null
    val canSave: Boolean
        get() = name.isNotBlank() && address.isNotBlank() && roles.isNotEmpty() && codepage != null

    fun toPrinter(newId: String): Printer = Printer(
        id = id ?: newId,
        name = name.trim(),
        link = link,
        address = address.trim(),
        paperWidthMm = paperWidthMm,
        codepage = codepage ?: 0,
        roles = roles,
    )
}

internal fun Printer.toRowUi() = PrinterRowUi(
    id = id,
    name = name,
    transport = link.name,
    address = address,
    roles = roles.joinToString(", ") { it.name },
)

internal fun Printer.toEditorUi() = PrinterEditorUi(
    id = id,
    name = name,
    link = link,
    address = address,
    paperWidthMm = paperWidthMm,
    codepageText = codepage.toString(),
    roles = roles,
)
