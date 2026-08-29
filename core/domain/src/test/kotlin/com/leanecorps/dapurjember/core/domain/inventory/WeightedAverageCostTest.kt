package com.leanecorps.dapurjember.core.domain.inventory

import com.leanecorps.dapurjember.core.common.money.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeightedAverageCostTest {

    @Test
    fun `blends the old and new cost by quantity`() {
        // 1000g @ 50/g + 1000g @ 70/g -> 60/g
        val avg = weightedAverageAfterPurchase(
            currentStockBase = 1_000.0,
            currentAvgCostPerBase = Money(50),
            purchasedQtyBase = 1_000.0,
            purchaseUnitCost = Money(70),
        )
        assertEquals(Money(60), avg)
    }

    @Test
    fun `a non-purchase (zero or negative qty) leaves the average untouched`() {
        assertEquals(
            Money(50),
            weightedAverageAfterPurchase(1_000.0, Money(50), purchasedQtyBase = 0.0, purchaseUnitCost = Money(999)),
        )
    }

    @Test
    fun `buying into a negative hole takes the purchase price`() {
        assertEquals(
            Money(80),
            weightedAverageAfterPurchase(-500.0, Money(10), purchasedQtyBase = 100.0, purchaseUnitCost = Money(80)),
        )
    }

    @Test
    fun `rounds to the nearest minor unit`() {
        // (3·100 + 1·151) / 4 = 112.75 -> 113
        val avg = weightedAverageAfterPurchase(3.0, Money(100), 1.0, Money(151))
        assertEquals(Money(113), avg)
    }
}
