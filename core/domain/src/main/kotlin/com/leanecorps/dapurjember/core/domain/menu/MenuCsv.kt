package com.leanecorps.dapurjember.core.domain.menu

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Parses a menu CSV (FR-M6). One row per orderable variant:
 *
 * ```
 * category,item,variant,price[,available]
 * Rice,Nasi Goreng Ayam,Regular,25000
 * Rice,Nasi Goreng Ayam,Large,30000
 * Drinks,Es Teh,,5000
 * ```
 *
 * A header row (first cell equals "category", case-insensitive) is skipped. An empty
 * `variant` cell becomes "Regular". `price` is major units, converted with [minorUnits].
 * Blank lines are ignored. This is a pure function — the caller persists [MenuCsvParseResult].
 */
object MenuCsv {

    private const val MIN_COLUMNS = 4

    fun parse(text: String, minorUnits: Int): MenuCsvParseResult {
        val rows = mutableListOf<MenuCsvRow>()
        val errors = mutableListOf<MenuCsvError>()

        text.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachIndexed

            val cells = line.split(',').map { it.trim() }
            if (index == 0 && cells.firstOrNull()?.equals("category", ignoreCase = true) == true) {
                return@forEachIndexed
            }
            if (cells.size < MIN_COLUMNS) {
                errors += MenuCsvError(index + 1, "expected at least $MIN_COLUMNS columns, got ${cells.size}")
                return@forEachIndexed
            }

            val category = cells[0]
            val item = cells[1]
            val variant = cells[2].ifBlank { "Regular" }
            val priceMinor = parsePrice(cells[3], minorUnits)
            val available = cells.getOrNull(4)?.let { parseBool(it) } ?: true

            when {
                category.isBlank() -> errors += MenuCsvError(index + 1, "category is blank")
                item.isBlank() -> errors += MenuCsvError(index + 1, "item is blank")
                priceMinor == null -> errors += MenuCsvError(index + 1, "invalid price '${cells[3]}'")
                else -> rows += MenuCsvRow(category, item, variant, priceMinor, available)
            }
        }
        return MenuCsvParseResult(rows, errors)
    }

    private fun parsePrice(raw: String, minorUnits: Int): Long? {
        val value = raw.replace(",", "").toBigDecimalOrNull()?.takeIf { it.signum() >= 0 } ?: return null
        return value.movePointRight(minorUnits).setScale(0, RoundingMode.HALF_UP).toLong()
    }

    private fun parseBool(raw: String): Boolean =
        raw.lowercase() !in setOf("no", "false", "0", "n", "unavailable", "sold out")

    private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()
}

data class MenuCsvRow(
    val category: String,
    val item: String,
    val variant: String,
    val priceMinor: Long,
    val available: Boolean,
)

data class MenuCsvError(val line: Int, val message: String)

data class MenuCsvParseResult(val rows: List<MenuCsvRow>, val errors: List<MenuCsvError>) {
    val categories: List<String> get() = rows.map { it.category }.distinct()
    val itemCount: Int get() = rows.map { it.category to it.item }.distinct().size
    val isUsable: Boolean get() = rows.isNotEmpty()
}
