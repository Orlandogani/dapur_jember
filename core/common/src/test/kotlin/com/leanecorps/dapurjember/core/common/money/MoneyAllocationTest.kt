package com.leanecorps.dapurjember.core.common.money

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MoneyAllocationTest {

    @Test
    fun `allocate sums back to the original exactly`() {
        val parts = Money(100).allocate(listOf(1, 1, 1))
        assertEquals(Money(100), parts.reduce(Money::plus))
    }

    @Test
    fun `allocate hands leftover minor units to the largest remainders first`() {
        // 100 split 3 ways: 33,33,33 base + 1 leftover -> first part gets it (ties by index)
        assertEquals(listOf(Money(34), Money(33), Money(33)), Money(100).allocate(listOf(1, 1, 1)))
    }

    @Test
    fun `allocate respects weights`() {
        assertEquals(listOf(Money(500), Money(300), Money(200)), Money(1_000).allocate(listOf(5, 3, 2)))
    }

    @Test
    fun `allocate distributes an uneven weighted split without losing a unit`() {
        val parts = Money(1_000).allocate(listOf(1, 1, 1, 1, 1, 1, 1)) // 142*7 = 994, 6 leftover
        assertEquals(Money(1_000), parts.reduce(Money::plus))
        assertEquals(listOf(143L, 143L, 143L, 143L, 143L, 143L, 142L), parts.map { it.minor })
    }

    @Test
    fun `allocate handles negative amounts`() {
        val parts = Money(-100).allocate(listOf(1, 1, 1))
        assertEquals(Money(-100), parts.reduce(Money::plus))
        assertEquals(listOf(-33L, -33L, -34L), parts.map { it.minor })
    }

    @Test
    fun `allocate ignores zero-weight parts`() {
        assertEquals(listOf(Money(100), Money.ZERO), Money(100).allocate(listOf(1, 0)))
    }

    @Test
    fun `splitEvenly puts the odd unit on the earliest piece`() {
        assertEquals(listOf(Money(4), Money(3), Money(3)), Money(10).splitEvenly(3))
    }

    @Test
    fun `allocate rejects empty weights, all-zero weights, and negative weights`() {
        assertThrows(IllegalArgumentException::class.java) { Money(10).allocate(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { Money(10).allocate(listOf(0, 0)) }
        assertThrows(IllegalArgumentException::class.java) { Money(10).allocate(listOf(1, -1)) }
    }

    @Test
    fun `splitEvenly rejects non-positive parts`() {
        assertThrows(IllegalArgumentException::class.java) { Money(10).splitEvenly(0) }
    }
}
