package com.leanecorps.dapurjember.core.domain.config

import com.leanecorps.dapurjember.core.common.id.UuidV7
import com.leanecorps.dapurjember.core.domain.auth.AuthRepository
import com.leanecorps.dapurjember.core.domain.auth.StaffRole
import com.leanecorps.dapurjember.core.domain.pricing.RoundingRule
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Everything the first-run wizard collects (FR-A4). */
data class SetupParams(
    val restaurantName: String,
    val currencyCode: String,
    val currencyMinorUnits: Int,
    val taxRateBasisPoints: Int,
    val taxInclusive: Boolean,
    val serviceChargeBasisPoints: Int,
    val serviceChargeTaxable: Boolean,
    val roundingRule: RoundingRule,
    val businessDayCutoffMinutes: Int,
    val timezoneId: String,
    val ownerName: String,
    val ownerPin: String,
)

/**
 * Writes the single `store_profile` row and creates the owner (FR-A4). Idempotent: a second
 * call once a profile exists is a no-op, and the owner is only created if none exists yet, so
 * a wizard that crashed mid-way can safely be re-run.
 */
class CompleteSetupUseCase @Inject constructor(
    private val storeProfiles: StoreProfileRepository,
    private val auth: AuthRepository,
) {
    suspend operator fun invoke(params: SetupParams) {
        if (storeProfiles.getProfile() != null) return

        val hasOwner = auth.observeActiveStaff().first().any { it.role == StaffRole.OWNER }
        if (!hasOwner) {
            auth.createStaff(params.ownerName, StaffRole.OWNER, params.ownerPin)
        }

        storeProfiles.upsertProfile(
            StoreProfile(
                id = UuidV7.generate(),
                name = params.restaurantName,
                currencyCode = params.currencyCode,
                currencyMinorUnits = params.currencyMinorUnits,
                taxRateBasisPoints = params.taxRateBasisPoints,
                taxInclusive = params.taxInclusive,
                serviceChargeBasisPoints = params.serviceChargeBasisPoints,
                serviceChargeTaxable = params.serviceChargeTaxable,
                roundingRule = params.roundingRule,
                businessDayCutoffMinutes = params.businessDayCutoffMinutes,
                timezoneId = params.timezoneId,
            ),
        )
    }
}
