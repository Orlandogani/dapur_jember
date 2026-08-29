package com.leanecorps.dapurjember.core.domain.auth

import kotlinx.coroutines.flow.Flow

enum class StaffRole { OWNER, MANAGER, CASHIER, WAITER }

data class Staff(
    val id: String,
    val name: String,
    val role: StaffRole,
    val active: Boolean = true,
)

/** Hashes and verifies staff PINs. Argon2id or bcrypt — never plaintext, never SHA-256 (arch §7). */
interface PinHasher {
    fun hash(pin: String): String
    fun verify(pin: String, hash: String): Boolean
}

interface AuthRepository {

    fun observeActiveStaff(): Flow<List<Staff>>

    /** Verifies the PIN and, on success, makes [staffId] the signed-in staff. */
    suspend fun signIn(staffId: String, pin: String): Boolean

    suspend fun signOut()

    /** Verifies a PIN without changing who is signed in — for step-up authorisation (FR-A3). */
    suspend fun verifyPin(staffId: String, pin: String): Boolean

    /** Creates a staff member (setup wizard / staff management). Returns the new id. */
    suspend fun createStaff(name: String, role: StaffRole, pin: String): String
}
