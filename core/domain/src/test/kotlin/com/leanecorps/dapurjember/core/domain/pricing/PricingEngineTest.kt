package com.leanecorps.dapurjember.core.domain.pricing

import com.leanecorps.dapurjember.core.common.money.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Worked examples, each cross-checked with a calculator. Amounts are IDR (0 minor units),
 * so `Money(15_000)` reads as Rp 15,000.
 */
class PricingEngineTest {

    private fun config(
        taxBp: Int = 0,
        taxMode: TaxMode = TaxMode.EXCLUSIVE,
        scBp: Int = 0,
        scTaxable: Boolean = false,
        rounding: RoundingRule = RoundingRule.NONE,
    ) = PricingConfig(
        tax = TaxConfig(taxBp, taxMode),
        serviceCharge = ServiceChargeConfig(scBp, scTaxable),
        rounding = rounding,
    )

    private fun line(price: Long, qty: Int, taxExempt: Boolean = false, discounts: List<Discount> = emptyList()) =
        PricingLine(Money(price), qty, taxExempt = taxExempt, discounts = discounts)

    @Test
    fun `two nasi goreng and an es teh with 10 percent exclusive tax`() {
        val bill = PricingEngine.price(
            PricingRequest(
                lines = listOf(line(15_000, 2), line(4_000, 1)),
                config = config(taxBp = 1_000),
            ),
        )

        assertEquals(Money(34_000), bill.subtotal)
        assertEquals(Money.ZERO, bill.discountTotal)
        assertEquals(Money(3_400), bill.tax)
        assertEquals(Money(37_400), bill.total)
        assertEquals(Money.ZERO, bill.roundingAdjustment)
    }

    @Test
    fun `same order with 10 percent inclusive tax leaves the total at the menu price`() {
        val bill = PricingEngine.price(
            PricingRequest(
                lines = listOf(line(15_000, 2), line(4_000, 1)),
                config = config(taxBp = 1_000, taxMode = TaxMode.INCLUSIVE),
            ),
        )

        assertEquals(Money(34_000), bill.subtotal)
        assertEquals(Money(3_091), bill.tax) // 34_000 - round(34_000 * 10000 / 11000)
        assertEquals(Money(34_000), bill.total)
    }

    @Test
    fun `10 percent bill discount then 5 percent taxable service charge, exclusive tax`() {
        val bill = PricingEngine.price(
            PricingRequest(
                lines = listOf(line(15_000, 2), line(4_000, 1)),
                billDiscounts = listOf(Discount.Percent(1_000, "promo")),
                config = config(taxBp = 1_000, scBp = 500, scTaxable = true),
            ),
        )

        assertEquals(Money(3_400), bill.billDiscount)
        assertEquals(Money(30_600), bill.discountedSubtotal)
        assertEquals(Money(1_530), bill.serviceCharge)
        assertEquals(Money(3_213), bill.tax) // (30_600 + 1_530) * 10%
        assertEquals(Money(35_343), bill.total)
    }

    @Test
    fun `a tax-exempt line is in the subtotal but out of the tax base`() {
        val bill = PricingEngine.price(
            PricingRequest(
                lines = listOf(line(15_000, 2), line(4_000, 1, taxExempt = true)),
                config = config(taxBp = 1_000),
            ),
        )

        assertEquals(Money(34_000), bill.subtotal)
        assertEquals(Money(3_000), bill.tax) // only on the 30_000
        assertEquals(Money(37_000), bill.total)
    }

    @Test
    fun `a fixed discount larger than the line is capped at the line total`() {
        val bill = PricingEngine.price(
            PricingRequest(
                lines = listOf(line(4_000, 1, discounts = listOf(Discount.Fixed(Money(5_000), "comp")))),
                config = config(),
            ),
        )

        assertEquals(Money(4_000), bill.lineDiscountTotal)
        assertEquals(Money.ZERO, bill.lines.single().net)
        assertEquals(Money.ZERO, bill.total)
    }

    @Test
    fun `NEAREST_100 rounding adjusts the final total and records the delta`() {
        val down = PricingEngine.price(
            PricingRequest(listOf(line(12_345, 1)), config = config(rounding = RoundingRule.NEAREST_100)),
        )
        assertEquals(Money(12_300), down.total)
        assertEquals(Money(-45), down.roundingAdjustment)

        val up = PricingEngine.price(
            PricingRequest(listOf(line(12_350, 1)), config = config(rounding = RoundingRule.NEAREST_100)),
        )
        assertEquals(Money(12_400), up.total)
        assertEquals(Money(50), up.roundingAdjustment)
    }

    @Test
    fun `inclusive and exclusive agree when the price is expressed consistently`() {
        val net = 100_000L
        val rateBp = 1_000

        val grossedUp = net + net / 10
        val exclusive = PricingEngine.price(
            PricingRequest(listOf(line(net, 1)), config = config(taxBp = rateBp)),
        )
        val inclusive = PricingEngine.price(
            PricingRequest(
                listOf(line(grossedUp, 1)),
                config = config(taxBp = rateBp, taxMode = TaxMode.INCLUSIVE),
            ),
        )

        assertEquals(Money(110_000), exclusive.total)
        assertEquals(exclusive.total, inclusive.total)
        assertEquals(exclusive.tax, inclusive.tax)
    }

    @Test
    fun `an empty order prices to zero`() {
        val bill = PricingEngine.price(PricingRequest(emptyList(), config = config(taxBp = 1_000, scBp = 500)))

        assertEquals(Money.ZERO, bill.subtotal)
        assertEquals(Money.ZERO, bill.serviceCharge)
        assertEquals(Money.ZERO, bill.tax)
        assertEquals(Money.ZERO, bill.total)
    }

    @Test
    fun `a line whose modifiers drop it below zero is rejected`() {
        val request = PricingRequest(
            lines = listOf(PricingLine(Money(4_000), quantity = 1, modifierDeltas = listOf(Money(-5_000)))),
            config = config(),
        )

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> { PricingEngine.price(request) }
    }
}
