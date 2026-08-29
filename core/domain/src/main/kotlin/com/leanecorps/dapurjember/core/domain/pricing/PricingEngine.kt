package com.leanecorps.dapurjember.core.domain.pricing

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.common.money.allocate
import java.math.BigDecimal

/**
 * The pure pricing calculation (`docs/2-architecture` 5.2). Order of operations is fixed and
 * load-bearing: line prices with modifiers → line discounts → bill discount → service charge
 * → tax → rounding. This is the highest-risk code in the app — every branch has a test.
 */
object PricingEngine {

    private const val BASIS_POINT_SCALE = 10_000L

    fun price(request: PricingRequest): PricedBill {
        val config = request.config

        val pricedLines = request.lines.map { line ->
            val grossUnit = line.modifierDeltas.fold(line.unitPrice) { acc, delta -> acc + delta }
            val gross = grossUnit * line.quantity
            require(!gross.isNegative) {
                "line gross is negative ($gross): unitPrice=${line.unitPrice}, modifiers=${line.modifierDeltas}"
            }
            val discount = totalDiscount(line.discounts, gross)
            PricedLine(gross = gross, discount = discount, net = gross - discount)
        }

        val subtotal = pricedLines.sumOfMoney { it.gross }
        val lineDiscountTotal = pricedLines.sumOfMoney { it.discount }
        val netSubtotal = pricedLines.sumOfMoney { it.net }

        val billDiscount = totalDiscount(request.billDiscounts, netSubtotal)
        val discountedSubtotal = netSubtotal - billDiscount

        val taxableGoods = taxableGoodsBase(pricedLines, request.lines, billDiscount, netSubtotal)

        val serviceCharge = discountedSubtotal.percent(config.serviceCharge.rateBasisPoints)
        val taxableServiceCharge = if (config.serviceCharge.taxable) serviceCharge else Money.ZERO

        val rate = config.tax.rateBasisPoints
        val tax: Money
        val preRounding: Money
        when (config.tax.mode) {
            TaxMode.EXCLUSIVE -> {
                tax = (taxableGoods + taxableServiceCharge).percent(rate)
                preRounding = discountedSubtotal + serviceCharge + tax
            }

            TaxMode.INCLUSIVE -> {
                val taxInGoods = taxableGoods - netOfInclusiveTax(taxableGoods, rate)
                val taxOnServiceCharge = taxableServiceCharge.percent(rate)
                tax = taxInGoods + taxOnServiceCharge
                preRounding = discountedSubtotal + serviceCharge + taxOnServiceCharge
            }
        }

        val total = config.rounding.apply(preRounding)
        val roundingAdjustment = total - preRounding

        require(!total.isNegative) { "pricing produced a negative total ($total) for $request" }

        return PricedBill(
            lines = pricedLines,
            subtotal = subtotal,
            lineDiscountTotal = lineDiscountTotal,
            billDiscount = billDiscount,
            discountedSubtotal = discountedSubtotal,
            serviceCharge = serviceCharge,
            tax = tax,
            roundingAdjustment = roundingAdjustment,
            total = total,
        )
    }

    /** Applies [discounts] in order to [base], never letting the running total exceed [base]. */
    private fun totalDiscount(discounts: List<Discount>, base: Money): Money {
        if (discounts.isEmpty() || base.isZero) return Money.ZERO
        var remaining = base
        var taken = Money.ZERO
        for (discount in discounts) {
            val raw = when (discount) {
                is Discount.Percent -> remaining.percent(discount.basisPoints)
                is Discount.Fixed -> discount.amount
            }
            val applied = raw.coerceIn(Money.ZERO, remaining)
            taken += applied
            remaining -= applied
        }
        return taken
    }

    /**
     * The base that tax applies to: the net of non-exempt lines, minus each line's
     * proportional share of the whole-bill discount (largest-remainder allocation).
     */
    private fun taxableGoodsBase(
        pricedLines: List<PricedLine>,
        lines: List<PricingLine>,
        billDiscount: Money,
        netSubtotal: Money,
    ): Money {
        if (pricedLines.isEmpty()) return Money.ZERO
        val allocation = if (billDiscount.isZero || netSubtotal.isZero) {
            List(pricedLines.size) { Money.ZERO }
        } else {
            billDiscount.allocate(pricedLines.map { it.net.minor })
        }
        var taxable = Money.ZERO
        pricedLines.forEachIndexed { index, line ->
            if (!lines[index].taxExempt) {
                taxable += line.net - allocation[index]
            }
        }
        return taxable
    }

    /** The tax-exclusive portion of an amount that already includes tax at [rateBasisPoints]. */
    private fun netOfInclusiveTax(inclusive: Money, rateBasisPoints: Int): Money {
        if (rateBasisPoints == 0 || inclusive.isZero) return inclusive
        val net = BigDecimal.valueOf(inclusive.minor)
            .multiply(BigDecimal.valueOf(BASIS_POINT_SCALE))
            .divide(BigDecimal.valueOf(BASIS_POINT_SCALE + rateBasisPoints), 0, Money.PERCENT_ROUNDING)
        return Money(net.longValueExact())
    }

    private inline fun <T> List<T>.sumOfMoney(selector: (T) -> Money): Money =
        fold(Money.ZERO) { acc, element -> acc + selector(element) }
}
