package com.leanecorps.dapurjember.core.domain.session

import kotlinx.coroutines.flow.Flow

/**
 * Who is working and on which till session. The order/payment/shift flows read this rather
 * than passing staff + shift ids around. `staffId` is set by the PIN screen; `shiftId` comes
 * from the open shift (there is at most one).
 */
data class Session(
    val staffId: String,
    val shiftId: String,
    val businessDay: String,
)

interface SessionRepository {

    /** Emits the current session, or `null` when no one is signed in or no shift is open. */
    fun observeSession(): Flow<Session?>

    suspend fun currentSession(): Session?

    /** The signed-in staff id even when no shift is open yet (the shift-open screen needs it). */
    fun observeCurrentStaffId(): Flow<String?>

    suspend fun currentStaffId(): String?

    /** Today's business day per the store's cutoff — usable before a shift exists. */
    suspend fun currentBusinessDay(): String

    /** Sets the signed-in staff member (PIN screen). Pass `null` to sign out. */
    suspend fun setCurrentStaff(staffId: String?)
}
