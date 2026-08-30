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

    suspend fun staffById(staffId: String): Staff?

    /** Creates a staff member (setup wizard / staff management). Returns the new id. */
    suspend fun createStaff(name: String, role: StaffRole, pin: String): String

    /** Every staff member including deactivated ones — the management screen (S29) shows both. */
    fun observeAllStaff(): Flow<List<Staff>>

    /** Renames / re-roles a staff member. Writes `audit_log` (CLAUDE.md rule 10). */
    suspend fun updateStaff(staffId: String, name: String, role: StaffRole, actorStaffId: String)

    /**
     * Deactivates or reactivates a staff member. Staff are never hard-deleted — their name
     * still has to resolve on historical orders and audit rows.
     */
    suspend fun setStaffActive(staffId: String, active: Boolean, actorStaffId: String)

    /** Sets a new PIN without knowing the old one — an owner resetting a forgotten PIN. */
    suspend fun resetPin(staffId: String, newPin: String, actorStaffId: String)
}
