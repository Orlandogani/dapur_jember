package com.leanecorps.dapurjember.core.printing

/** Thermal paper widths RestoPOS supports (FR-PR4). [columns] is the Font-A character count. */
enum class PaperWidth(val millimetres: Int, val columns: Int) {
    MM_58(58, 32),
    MM_80(80, 48),
    ;

    companion object {
        fun ofMillimetres(mm: Int): PaperWidth = entries.firstOrNull { it.millimetres == mm } ?: MM_80
    }
}

/** What a configured printer prints (FR-PR2). A single device may hold several roles. */
enum class PrinterRole { KITCHEN, BAR, RECEIPT }

/** Physical link to a printer (architecture §6). */
enum class PrinterTransport { BLUETOOTH, USB, TCP }
