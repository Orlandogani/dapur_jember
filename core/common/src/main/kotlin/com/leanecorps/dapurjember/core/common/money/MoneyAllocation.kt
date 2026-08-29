package com.leanecorps.dapurjember.core.common.money

/**
 * Distributes this amount across [weights] using the largest-remainder method: each part
 * gets `floor(amount * weight / totalWeight)` minor units, then the leftover units are
 * handed out one each to the parts with the largest fractional remainder (ties broken by
 * position). The result always sums back to this amount exactly — no minor unit is created
 * or lost. Works for negative amounts too (`floorDiv`/`floorMod` keep the leftover in
 * `0 until weights.size`).
 *
 * All weights must be `>= 0` and their sum must be `> 0`.
 */
fun Money.allocate(weights: List<Long>): List<Money> {
    require(weights.isNotEmpty()) { "weights must not be empty" }
    require(weights.all { it >= 0L }) { "weights must be non-negative: $weights" }
    val totalWeight = weights.sumOf { it }
    require(totalWeight > 0L) { "the sum of weights must be positive" }

    val shares = LongArray(weights.size)
    val remainderByIndex = LongArray(weights.size)
    var distributed = 0L
    weights.forEachIndexed { index, weight ->
        val numerator = minor * weight
        shares[index] = Math.floorDiv(numerator, totalWeight)
        remainderByIndex[index] = Math.floorMod(numerator, totalWeight)
        distributed += shares[index]
    }

    val leftover = (minor - distributed).toInt() // always in 0 until weights.size
    weights.indices
        .sortedWith(compareByDescending<Int> { remainderByIndex[it] }.thenBy { it })
        .take(leftover)
        .forEach { shares[it] += 1L }

    return shares.map { Money(it) }
}

/**
 * Splits this amount into [parts] pieces as evenly as possible; the pieces sum back to this
 * amount exactly, with any odd minor units going to the earliest pieces.
 */
fun Money.splitEvenly(parts: Int): List<Money> {
    require(parts > 0) { "parts must be positive, was $parts" }
    return allocate(List(parts) { 1L })
}
