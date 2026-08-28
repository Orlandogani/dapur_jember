package com.leanecorps.dapurjember.core.common.id

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random

class UuidV7Test {

    @Test
    fun `generates a well-formed version 7 uuid`() {
        val id = UuidV7.generate()
        val uuid = UUID.fromString(id) // throws if malformed
        assertEquals(7, uuid.version())
        assertEquals(2, uuid.variant()) // RFC 4122 variant (0b10)
    }

    @Test
    fun `is lexically sortable by timestamp`() {
        val early = UuidV7.generate(1_000L, Random(42))
        val late = UuidV7.generate(2_000L, Random(42))
        assertTrue(early < late, "$early should sort before $late")
    }

    @Test
    fun `same millisecond still produces distinct ids`() {
        val rng = Random(1)
        val first = UuidV7.generate(1_724_800_000_000L, rng)
        val second = UuidV7.generate(1_724_800_000_000L, rng)
        assertNotEquals(first, second)
    }

    @Test
    fun `generates unique ids in bulk`() {
        val ids = HashSet<String>()
        repeat(10_000) { ids.add(UuidV7.generate()) }
        assertEquals(10_000, ids.size)
    }
}
