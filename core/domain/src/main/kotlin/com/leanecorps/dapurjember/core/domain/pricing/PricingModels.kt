package com.leanecorps.dapurjember.core.domain.pricing

import com.leanecorps.dapurjember.core.common.money.Money

/** One order line as priced input: a unit price plus modifier deltas, quantity, discounts. */
data class PricingLine(
    val unitPrice: Money,
    val quantity: Int,
    val modifierDeltas: List<Money> = emptyList(),
    val taxExempt: Boolean = false,
    val discounts: List<Discount> = emptyList(),
) {
    init {
        require(quantity >= 0) { "quantity must be >= 0, was $quantity" }
    }
}

data class TaxConfig(val rateBasisPoints: Int, val mode: TaxMode) {
    init {
        require(rateBasisPoints >= 0) { "tax rate must be >= 0, was $rateBasisPoints" }
    }

    companion object {
        val NONE = TaxConfig(rateBasisPoints = 0, mode = TaxMode.EXCLUSIVE)
    }
}

data class ServiceChargeConfig(val rateBasisPoints: Int, val taxable: Boolean) {
    init {
        require(rateBasisPoints >= 0) { "service charge rate must be >= 0, was $rateBasisPoints" }
    }

    companion object {
        val NONE = ServiceChargeConfig(rateBasisPoints = 0, taxable = false)
    }
}

data class PricingConfig(
    val tax: TaxConfig,
    val serviceCharge: ServiceChargeConfig = ServiceChargeConfig.NONE,
    val rounding: RoundingRule = RoundingRule.NONE,
)

data class PricingRequest(
    val lines: List<PricingLine>,
    val billDiscounts: List<Discount> = emptyList(),
    val config: PricingConfig,
)

/** A single line after pricing: `net = gross − discount`. */
data class PricedLine(
    val gross: Money,
    val discount: Money,
    val net: Money,
)

/**
 * The fully computed bill. Fields map 1:1 onto the `orders` denormalised snapshot columns
 * (`docs/3-data-model` 3.5); [discountTotal] is what goes in `orders.discount_minor`.
 */
data class PricedBill(
    val lines: List<PricedLine>,
    val subtotal: Money,
    val lineDiscountTotal: Money,
    val billDiscount: Money,
    val discountedSubtotal: Money,
    val serviceCharge: Money,
    val tax: Money,
    val roundingAdjustment: Money,
    val total: Money,
) {
    /** Total of every discount, matching `orders.discount_minor`. */
    val discountTotal: Money get() = lineDiscountTotal + billDiscount

    /** The total before the rounding rule was applied. */
    val preRoundingTotal: Money get() = total - roundingAdjustment
}
