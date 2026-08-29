package com.leanecorps.dapurjember.core.domain.pricing

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.common.money.splitEvenly
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PricingEnginePropertyTest {

    private val money = Arb.long(0L..1_000_000L).map { Money(it) }

    private val discount: Arb<Discount> = Arb.choice(
        Arb.int(0..10_000).map { Discount.Percent(it, "pct") },
        Arb.long(0L..200_000L).map { Discount.Fixed(Money(it), "fixed") },
    )

    private val line: Arb<PricingLine> = arbitrary {
        PricingLine(
            unitPrice = money.bind(),
            quantity = Arb.int(0..20).bind(),
            modifierDeltas = Arb.list(Arb.long(0L..5_000L).map { Money(it) }, 0..3).bind(),
            taxExempt = Arb.boolean().bind(),
            discounts = Arb.list(discount, 0..2).bind(),
        )
    }

    private val request: Arb<PricingRequest> = arbitrary {
        PricingRequest(
            lines = Arb.list(line, 0..6).bind(),
            billDiscounts = Arb.list(discount, 0..2).bind(),
            config = PricingConfig(
                tax = TaxConfig(Arb.int(0..2_500).bind(), Arb.enum<TaxMode>().bind()),
                serviceCharge = ServiceChargeConfig(Arb.int(0..1_500).bind(), Arb.boolean().bind()),
                rounding = Arb.enum<RoundingRule>().bind(),
            ),
        )
    }

    @Test
    fun `pricing invariants hold across the input space`() = runBlocking<Unit> {
        checkAll(request) { req ->
            val bill = PricingEngine.price(req)

            assertTrue(bill.total >= Money.ZERO) { "negative total for $req" }
            assertTrue(bill.discountedSubtotal >= Money.ZERO)
            assertTrue(bill.lineDiscountTotal <= bill.subtotal) { "line discounts exceed subtotal: $req" }

            val netSubtotal = bill.subtotal - bill.lineDiscountTotal
            assertTrue(bill.billDiscount <= netSubtotal) { "bill discount exceeds net subtotal: $req" }
            bill.lines.forEach { assertTrue(it.net >= Money.ZERO) }

            assertEquals(bill.total, req.config.rounding.apply(bill.preRoundingTotal))

            when (req.config.rounding) {
                RoundingRule.NONE, RoundingRule.NEAREST_1 ->
                    assertEquals(Money.ZERO, bill.roundingAdjustment)

                RoundingRule.NEAREST_5 ->
                    assertTrue(bill.roundingAdjustment.abs() < Money(5))

                RoundingRule.NEAREST_100 ->
                    assertTrue(bill.roundingAdjustment.abs() < Money(100))
            }
        }
    }

    @Test
    fun `the total always splits into parts that sum back exactly`() = runBlocking<Unit> {
        checkAll(request, Arb.int(1..8)) { req, parts ->
            val total = PricingEngine.price(req).total
            assertEquals(total, total.splitEvenly(parts).reduce(Money::plus))
        }
    }
}
