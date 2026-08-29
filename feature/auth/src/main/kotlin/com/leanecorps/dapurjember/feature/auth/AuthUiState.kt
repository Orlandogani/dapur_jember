package com.leanecorps.dapurjember.feature.auth

import com.leanecorps.dapurjember.core.domain.auth.Staff

data class AuthUiState(
    val loading: Boolean = true,
    val staff: List<Staff> = emptyList(),
    val selectedStaffId: String? = null,
    val pin: String = "",
    val error: Boolean = false,
    val signedIn: Boolean = false,
) {
    /** True on a fresh install — no staff exist, so the first PIN creates the owner (FR-A4). */
    val firstRun: Boolean get() = !loading && staff.isEmpty()

    val canSubmit: Boolean get() = pin.length in PIN_MIN..PIN_MAX && (firstRun || selectedStaffId != null)

    companion object {
        const val PIN_MIN = 4
        const val PIN_MAX = 6
    }
}
