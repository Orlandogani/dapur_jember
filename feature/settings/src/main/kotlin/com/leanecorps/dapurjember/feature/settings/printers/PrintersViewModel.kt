package com.leanecorps.dapurjember.feature.settings.printers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.domain.printing.PrinterLink
import com.leanecorps.dapurjember.core.domain.printing.PrinterRepository
import com.leanecorps.dapurjember.core.domain.printing.PrinterRole
import com.leanecorps.dapurjember.core.domain.printing.TicketPrinter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private val TEST_STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@HiltViewModel
class PrintersViewModel @Inject constructor(
    private val printers: PrinterRepository,
    private val ticketPrinter: TicketPrinter,
) : ViewModel() {

    private val editor = MutableStateFlow<PrinterEditorUi?>(null)
    private val testQueued = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PrintersUiState> = combine(
        printers.observePrinters(),
        editor,
        testQueued,
    ) { list, editorState, msg ->
        PrintersUiState(
            loading = false,
            printers = list.map { it.toRowUi() },
            editor = editorState,
            testPageQueuedFor = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PrintersUiState())

    fun startAdd() {
        editor.value = PrinterEditorUi()
    }

    fun startEdit(id: String) {
        viewModelScope.launch { editor.value = printers.getPrinter(id)?.toEditorUi() }
    }

    fun closeEditor() {
        editor.value = null
    }

    fun edit(transform: (PrinterEditorUi) -> PrinterEditorUi) {
        editor.value = editor.value?.let(transform)
    }

    fun toggleRole(role: PrinterRole) = edit { current ->
        current.copy(roles = if (role in current.roles) current.roles - role else current.roles + role)
    }

    fun setLink(link: PrinterLink) = edit { it.copy(link = link) }

    fun save() {
        val draft = editor.value ?: return
        if (!draft.canSave) return
        viewModelScope.launch {
            printers.savePrinter(draft.toPrinter(UuidV7.generate()))
            editor.value = null
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { printers.removePrinter(id) }
    }

    fun testPrint(id: String) {
        viewModelScope.launch {
            val printer = printers.getPrinter(id) ?: return@launch
            ticketPrinter.printTestPage(
                printerId = printer.id,
                printerName = printer.name,
                paperWidthMm = printer.paperWidthMm,
                printedAt = LocalDateTime.now().format(TEST_STAMP),
            )
            testQueued.value = printer.name
        }
    }

    fun dismissMessage() {
        testQueued.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
