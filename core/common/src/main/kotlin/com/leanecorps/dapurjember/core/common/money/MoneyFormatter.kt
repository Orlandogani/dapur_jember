package com.leanecorps.dapurjember.core.common.money

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Renders [money] as `"<CODE> <grouped amount>"`, e.g.
 * `formatMoney(Money(1_500_000), "IDR", 0)` -> `"IDR 1,500,000"` and
 * `formatMoney(Money(5), "USD", 2)` -> `"USD 0.05"`.
 *
 * Locale-independent on purpose: `,` groups thousands, `.` is the decimal separator, so the
 * output is stable across devices and trivial to assert on. Receipt templates localise
 * later; this is the canonical debug/report form.
 *
 * @param currencyCode ISO 4217 code, used verbatim as the prefix.
 * @param minorUnits digits after the decimal point for this currency (2 for USD, 0 for IDR).
 */
fun formatMoney(money: Money, currencyCode: String, minorUnits: Int): String {
    require(minorUnits >= 0) { "minorUnits must be >= 0, was $minorUnits" }

    val amount = BigDecimal.valueOf(money.minor).movePointLeft(minorUnits)
    val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    val pattern = if (minorUnits == 0) "#,##0" else "#,##0." + "0".repeat(minorUnits)
    val formatted = DecimalFormat(pattern, symbols).format(amount)
    return "$currencyCode $formatted"
}
