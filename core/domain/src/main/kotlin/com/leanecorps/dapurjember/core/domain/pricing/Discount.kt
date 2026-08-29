package com.leanecorps.dapurjember.core.domain.pricing

import com.leanecorps.dapurjember.core.common.money.Money

/**
 * A discount on a single line or on the whole bill (FR-P4). Always carries a [reason];
 * the calculation never lets the total of all discounts exceed the base they apply to.
 */
sealed interface Discount {
    val reason: String

    /** [basisPoints] of the base amount; 10 000 bp = 100%. */
    data class Percent(val basisPoints: Int, override val reason: String) : Discount {
        init {
            require(basisPoints >= 0) { "discount basisPoints must be >= 0, was $basisPoints" }
        }
    }

    /** A flat amount, capped at the base it is applied to. */
    data class Fixed(val amount: Money, override val reason: String) : Discount {
        init {
            require(!amount.isNegative) { "fixed discount must be >= 0, was $amount" }
        }
    }
}
