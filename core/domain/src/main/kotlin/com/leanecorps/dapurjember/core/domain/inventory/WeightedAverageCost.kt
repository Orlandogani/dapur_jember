package com.leanecorps.dapurjember.core.domain.inventory

import com.leanecorps.dapurjember.core.common.money.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Weighted-average cost after a purchase (FR-I5). Only a stock-increasing purchase moves the
 * average; anything else (waste, count correction, sale) leaves it unchanged.
 *
 * `new = (currentStock·currentAvg + purchasedQty·purchaseUnitCost) / (currentStock + purchasedQty)`
 *
 * If the resulting total quantity is `<= 0` (buying into a negative hole), the purchase price
 * becomes the new average — the old negative-weighted term is meaningless.
 */
fun weightedAverageAfterPurchase(
    currentStockBase: Double,
    currentAvgCostPerBase: Money,
    purchasedQtyBase: Double,
    purchaseUnitCost: Money,
): Money {
    val totalQty = currentStockBase + purchasedQtyBase
    return when {
        purchasedQtyBase <= 0.0 -> currentAvgCostPerBase
        totalQty <= 0.0 -> purchaseUnitCost
        else -> {
            val currentValue =
                BigDecimal.valueOf(currentStockBase) * BigDecimal.valueOf(currentAvgCostPerBase.minor)
            val purchasedValue =
                BigDecimal.valueOf(purchasedQtyBase) * BigDecimal.valueOf(purchaseUnitCost.minor)
            val avg = (currentValue + purchasedValue)
                .divide(BigDecimal.valueOf(totalQty), 0, RoundingMode.HALF_UP)
            Money(avg.toLong())
        }
    }
}
