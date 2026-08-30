package com.leanecorps.dapurjember.core.domain.auth

import com.leanecorps.dapurjember.core.domain.session.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Step-up authorisation (FR-A3). A privileged action first asks whether the *signed-in* staff
 * may do it; if not, a manager or owner authorises it with their PIN without anyone being
 * logged out. Whoever ends up authorising is returned, so the caller can record them on the
 * `audit_log` row.
 */
class AuthoriseUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val session: SessionRepository,
) {

    /** True when the signed-in staff already holds [permission] — no step-up dialog needed. */
    suspend fun currentUserCan(permission: Permission): Boolean {
        val staffId = session.currentStaffId() ?: return false
        return auth.staffById(staffId)?.role?.can(permission) == true
    }

    /**
     * Verifies [pin] against every active staff member who holds [permission], returning the
     * one that matched, or `null` if none did. Checking against all holders (rather than
     * making the user first pick their name) keeps the dialog to a single PIN entry.
     */
    suspend fun authorise(permission: Permission, pin: String): Staff? =
        auth.observeActiveStaff().first()
            .filter { it.role.can(permission) }
            .firstOrNull { auth.verifyPin(it.id, pin) }

    /**
     * Resolves who should be recorded as the actor: the signed-in staff when they already
     * hold [permission], otherwise whoever the step-up [pin] authorises. `null` means the
     * action is not permitted and the caller must not proceed.
     */
    suspend fun actorFor(permission: Permission, pin: String?): String? {
        if (currentUserCan(permission)) return session.currentStaffId()
        return pin?.let { authorise(permission, it)?.id }
    }
}
