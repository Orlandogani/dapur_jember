package com.leanecorps.dapurjember.core.common.money

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoneyTest {

    @Test
    fun `plus and minus`() {
        assertEquals(Money(300), Money(100) + Money(200))
        assertEquals(Money(-100), Money(100) - Money(200))
    }

    @Test
    fun `unary minus and abs`() {
        assertEquals(Money(-50), -Money(50))
        assertEquals(Money(50), Money(-50).abs())
        assertEquals(Money(50), Money(50).abs())
    }

    @Test
    fun `times by int and long`() {
        assertEquals(Money(600), Money(200) * 3)
        assertEquals(Money(600), Money(200) * 3L)
        assertEquals(Money(-600), Money(200) * -3)
    }

    @Test
    fun `plus overflow throws instead of wrapping`() {
        assertThrows(ArithmeticException::class.java) { Money(Long.MAX_VALUE) + Money(1) }
    }

    @Test
    fun `times overflow throws instead of wrapping`() {
        assertThrows(ArithmeticException::class.java) { Money(Long.MAX_VALUE) * 2 }
    }

    @Test
    fun `compareTo and predicates order by minor units`() {
        assertTrue(Money(100) < Money(200))
        assertTrue(Money(-1) < Money.ZERO)
        assertEquals(0, Money(5).compareTo(Money(5)))
        assertTrue(Money.ZERO.isZero)
        assertTrue(Money(1).isPositive)
        assertTrue(Money(-1).isNegative)
        assertFalse(Money.ZERO.isPositive)
    }

    @Test
    fun `percent is identity at 0 and 10000 basis points`() {
        assertEquals(Money.ZERO, Money(12_345).percent(0))
        assertEquals(Money(12_345), Money(12_345).percent(10_000))
    }

    @Test
    fun `percent uses bankers rounding at ties`() {
        assertEquals(Money(0), Money(1).percent(5_000)) // 0.5 -> 0 (even)
        assertEquals(Money(2), Money(3).percent(5_000)) // 1.5 -> 2 (even)
        assertEquals(Money(2), Money(5).percent(5_000)) // 2.5 -> 2 (even)
        assertEquals(Money(0), Money(-1).percent(5_000)) // -0.5 -> 0 (even)
        assertEquals(Money(-2), Money(-3).percent(5_000)) // -1.5 -> -2 (even)
    }

    @Test
    fun `percent rounds non-tie values normally`() {
        assertEquals(Money(206), Money(2_500).percent(825)) // 206.25 -> 206
        assertEquals(Money(207), Money(2_509).percent(825)) // 206.9925 -> 207
    }

    @Test
    fun `percent on large values has no floating point drift`() {
        // 8.25% of 9_000_000_000_000 is exactly 742_500_000_000
        assertEquals(Money(742_500_000_000), Money(9_000_000_000_000).percent(825))
    }
}
