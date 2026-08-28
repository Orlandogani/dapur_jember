package com.leanecorps.dapurjember.core.common.id

import java.util.UUID
import kotlin.random.Random

/**
 * Generates RFC 9562 version-7 UUIDs: a 48-bit big-endian Unix-millisecond timestamp
 * followed by 74 random bits, with the version (7) and variant (0b10) fields set.
 *
 * v7 IDs are time-sortable, which keeps database B-tree index locality reasonable as the
 * orders table grows (docs/2-architecture section 4.3). Every primary key in the app is
 * one of these, generated client-side — never a Room auto-increment.
 */
object UuidV7 {

    private const val TIMESTAMP_MASK = 0xFFFF_FFFF_FFFFL
    private const val VERSION_7 = 0x7L
    private const val RAND_A_BITS = 0x1000 // 12 bits
    private const val LOW_62_BITS = 0x3FFF_FFFF_FFFF_FFFFL
    private const val VARIANT_10 = 0b10L

    fun generate(): String = generate(System.currentTimeMillis(), Random.Default)

    internal fun generate(unixMillis: Long, random: Random): String {
        require(unixMillis >= 0L) { "unixMillis must be >= 0, was $unixMillis" }

        val timestamp = unixMillis and TIMESTAMP_MASK
        val randA = random.nextInt(RAND_A_BITS).toLong()
        val randB = random.nextLong()

        val mostSignificantBits =
            (timestamp shl 16) or (VERSION_7 shl 12) or randA
        val leastSignificantBits =
            (VARIANT_10 shl 62) or (randB and LOW_62_BITS)

        return UUID(mostSignificantBits, leastSignificantBits).toString()
    }
}
