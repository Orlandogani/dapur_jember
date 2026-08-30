package com.leanecorps.dapurjember.core.domain.order

import com.leanecorps.dapurjember.core.common.money.Money
import com.leanecorps.dapurjember.core.common.money.allocate
import com.leanecorps.dapurjember.core.common.money.splitEvenly

/**
 * Splitting a bill (S09, FR-T2). Three modes, one invariant that matters more than any of
 * them: **the parts always sum to exactly the order total.** A split that loses or invents a
 * minor unit produces a till that does not reconcile, which is the fastest way to lose a
 * restaurant's trust.
 *
 * Splitting never writes anything — it only proposes amounts. The parts are then settled as
 * ordinary partial payments (FR-P3), so one order can be split three ways and paid with three
 * different methods without any new persistence concept.
 */
object BillSplit {

    /**
     * One guest's share. Identified by index, not by a display name — formatting "Guest 1"
     * is a UI concern and must stay out of the domain so it can be localised (NFR8).
     */
    data class Part(val guestIndex: Int, val amount: Money)

    /**
     * Splits [total] into [ways] equal parts. Odd minor units go to the earliest parts, so
     * for 10.00 across 3 people you get 3.34 / 3.33 / 3.33 rather than three 3.33s and a
     * penny nobody pays.
     */
    fun evenly(total: Money, ways: Int): List<Part> {
        require(ways > 0) { "ways must be positive, was $ways" }
        return total.splitEvenly(ways).mapIndexed { index, amount -> Part(index, amount) }
    }

    /**
     * Splits [total] in proportion to what each guest ordered. [weightsByGuest] is each
     * guest's share of the *line* value; the whole-bill extras (tax, service, rounding,
     * bill-level discount) are distributed across guests in the same proportion, so the parts
     * still sum to the real total rather than to the sum of the lines.
     *
     * A guest who ordered nothing is dropped — charging them 0 would just add a payment step.
     */
    fun byItem(total: Money, weightsByGuest: Map<Int, Money>): List<Part> {
        val ordering = weightsByGuest.filterValues { it.minor > 0L }
        require(ordering.isNotEmpty()) { "at least one guest must have something to pay for" }

        val guests = ordering.keys.sorted()
        val amounts = total.allocate(guests.map { ordering.getValue(it).minor })
        return guests.mapIndexed { index, guest -> Part(guest, amounts[index]) }
    }

    /**
     * Validates a set of hand-entered amounts against [total] (split by amount). Returns the
     * remainder: zero means the split is exactly settled, positive means still owing,
     * negative means the entered amounts overshoot. The UI must refuse a non-zero remainder —
     * a bill that is "nearly" split is a variance in someone's till at close.
     */
    fun remainderAfter(total: Money, entered: List<Money>): Money =
        entered.fold(total) { left, part -> left - part }
}
