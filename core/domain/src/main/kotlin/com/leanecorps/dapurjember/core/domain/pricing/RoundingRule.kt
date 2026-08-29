package com.leanecorps.dapurjember.core.domain.pricing

import com.leanecorps.dapurjember.core.common.money.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Configurable final-total rounding (`store_profile.rounding_mode`, FR-P6). Operates on
 * `Money.minor`, so [NEAREST_100] on IDR (0 minor units) rounds to the nearest 100 rupiah,
 * and [NEAREST_5] on a 2-decimal currency rounds to the nearest 5 cents.
 */
enum class RoundingRule(private val step: Long) {
    NONE(1L),
    NEAREST_1(1L),
    NEAREST_5(5L),
    NEAREST_100(100L),
    ;

    /** Rounds [amount] to the nearest multiple of the step; exact halves round away from zero. */
    fun apply(amount: Money): Money {
        if (step <= 1L) return amount
        val quotient = BigDecimal.valueOf(amount.minor)
            .divide(BigDecimal.valueOf(step), 0, RoundingMode.HALF_UP)
        return Money(quotient.multiply(BigDecimal.valueOf(step)).longValueExact())
    }
}
