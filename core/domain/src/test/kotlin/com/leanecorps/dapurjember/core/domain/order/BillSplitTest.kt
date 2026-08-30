package com.leanecorps.dapurjember.core.domain.order

import com.leanecorps.dapurjember.core.common.money.Money
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BillSplitTest {

    // --- Evenly ---

    @Test
    fun `an even split hands the odd minor units to the earliest guests`() {
        val parts = BillSplit.evenly(Money(1_000), ways = 3)
        assertEquals(listOf(334L, 333L, 333L), parts.map { it.amount.minor })
        assertEquals(listOf(0, 1, 2), parts.map { it.guestIndex })
    }

    @Test
    fun `an even split of an exact multiple has no remainder`() {
        assertEquals(listOf(500L, 500L), BillSplit.evenly(Money(1_000), ways = 2).map { it.amount.minor })
    }

    @Test
    fun `splitting zero ways is rejected rather than silently returning nothing`() {
        assertThrows(IllegalArgumentException::class.java) { BillSplit.evenly(Money(1_000), ways = 0) }
    }

    // --- By item ---

    @Test
    fun `by-item spreads the whole-bill extras in proportion to what each guest ordered`() {
        // Lines: A 60_000, B 30_000. Total 108_900 after 10% tax and rounding.
        val parts = BillSplit.byItem(
            total = Money(108_900),
            weightsByGuest = mapOf(0 to Money(60_000), 1 to Money(30_000)),
        )
        assertEquals(listOf(0, 1), parts.map { it.guestIndex })
        assertEquals(listOf(72_600L, 36_300L), parts.map { it.amount.minor })
        assertEquals(108_900L, parts.sumOf { it.amount.minor })
    }

    @Test
    fun `a guest who ordered nothing is left out of the split`() {
        val parts = BillSplit.byItem(
            total = Money(10_000),
            weightsByGuest = mapOf(0 to Money(10_000), 1 to Money.ZERO),
        )
        assertEquals(listOf(0), parts.map { it.guestIndex })
        assertEquals(10_000L, parts.single().amount.minor)
    }

    @Test
    fun `a split where nobody ordered anything is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            BillSplit.byItem(Money(10_000), mapOf(0 to Money.ZERO))
        }
    }

    // --- By amount ---

    @Test
    fun `remainderAfter reports what is still owing, or an overshoot as negative`() {
        assertEquals(Money(2_000), BillSplit.remainderAfter(Money(10_000), listOf(Money(8_000))))
        assertEquals(Money.ZERO, BillSplit.remainderAfter(Money(10_000), listOf(Money(6_000), Money(4_000))))
        assertEquals(Money(-500), BillSplit.remainderAfter(Money(10_000), listOf(Money(10_500))))
    }

    // --- The invariant that decides whether the till reconciles ---

    @Test
    fun `an even split always sums back to the total, for any total and any number of ways`() = runBlocking<Unit> {
        checkAll(Arb.long(-1_000_000L..1_000_000L), Arb.int(1..12)) { totalMinor, ways ->
            val parts = BillSplit.evenly(Money(totalMinor), ways)
            check(parts.size == ways)
            check(parts.sumOf { it.amount.minor } == totalMinor)
        }
    }

    @Test
    fun `a by-item split always sums back to the total, whatever the weights`() = runBlocking<Unit> {
        checkAll(
            Arb.long(0L..1_000_000L),
            Arb.long(1L..500_000L),
            Arb.long(1L..500_000L),
            Arb.long(1L..500_000L),
        ) { totalMinor, a, b, c ->
            val parts = BillSplit.byItem(
                total = Money(totalMinor),
                weightsByGuest = mapOf(0 to Money(a), 1 to Money(b), 2 to Money(c)),
            )
            check(parts.sumOf { it.amount.minor } == totalMinor)
        }
    }

    @Test
    fun `a three-way split paid in full leaves nothing owing (roadmap M2 exit criterion)`() {
        val total = Money(100_000)
        val parts = BillSplit.evenly(total, ways = 3)
        assertEquals(Money.ZERO, BillSplit.remainderAfter(total, parts.map { it.amount }))
    }
}
