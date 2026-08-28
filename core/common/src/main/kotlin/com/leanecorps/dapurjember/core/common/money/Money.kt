package com.leanecorps.dapurjember.core.common.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A monetary amount held as a whole number of **minor units** (sen, cents, fen).
 *
 * Money is never a `Double` or `Float` anywhere in this codebase — floating-point money
 * produces receipts that do not add up. The currency and its minor-unit scale live on the
 * store profile, not here; [Money] only knows the integer count.
 */
@JvmInline
value class Money(val minor: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(Math.addExact(minor, other.minor))

    operator fun minus(other: Money): Money = Money(Math.subtractExact(minor, other.minor))

    operator fun unaryMinus(): Money = Money(Math.negateExact(minor))

    operator fun times(factor: Long): Money = Money(Math.multiplyExact(minor, factor))

    operator fun times(factor: Int): Money = times(factor.toLong())

    override fun compareTo(other: Money): Int = minor.compareTo(other.minor)

    fun abs(): Money = if (minor < 0L) Money(Math.negateExact(minor)) else this

    val isZero: Boolean get() = minor == 0L
    val isPositive: Boolean get() = minor > 0L
    val isNegative: Boolean get() = minor < 0L

    /**
     * [basisPoints] of this amount, rounded to whole minor units with [PERCENT_ROUNDING].
     * 10 000 basis points = 100%. Exact `BigDecimal` math — no floating point.
     *
     * e.g. `Money(2_500).percent(825)` (8.25% of 25.00) == `Money(206)`.
     */
    fun percent(basisPoints: Int): Money {
        val scaled = BigDecimal.valueOf(minor)
            .multiply(BigDecimal.valueOf(basisPoints.toLong()))
            .divide(BASIS_POINT_DIVISOR, 0, PERCENT_ROUNDING)
        return Money(scaled.longValueExact())
    }

    override fun toString(): String = "Money($minor)"

    companion object {
        val ZERO = Money(0L)

        /** Tie-breaking rule for [percent]: banker's rounding — ties go to the even unit. */
        val PERCENT_ROUNDING: RoundingMode = RoundingMode.HALF_EVEN

        private val BASIS_POINT_DIVISOR: BigDecimal = BigDecimal.valueOf(10_000L)
    }
}
