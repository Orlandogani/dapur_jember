package com.leanecorps.dapurjember.core.domain.config

import com.leanecorps.dapurjember.core.domain.pricing.PricingConfig
import com.leanecorps.dapurjember.core.domain.pricing.RoundingRule
import com.leanecorps.dapurjember.core.domain.pricing.ServiceChargeConfig
import com.leanecorps.dapurjember.core.domain.pricing.TaxConfig
import com.leanecorps.dapurjember.core.domain.pricing.TaxMode
import kotlinx.coroutines.flow.Flow

/** The single-row restaurant configuration (`docs/3-data-model` §3.1). */
data class StoreProfile(
    val id: String,
    val name: String,
    val currencyCode: String,
    val currencyMinorUnits: Int,
    val taxRateBasisPoints: Int,
    val taxInclusive: Boolean,
    val serviceChargeBasisPoints: Int,
    val serviceChargeTaxable: Boolean,
    val roundingRule: RoundingRule,
    val businessDayCutoffMinutes: Int,
    val timezoneId: String,
    val receiptHeader: String? = null,
    val receiptFooter: String? = null,
) {
    /** The pricing engine's config derived from this profile. */
    fun pricingConfig(): PricingConfig = PricingConfig(
        tax = TaxConfig(
            rateBasisPoints = taxRateBasisPoints,
            mode = if (taxInclusive) TaxMode.INCLUSIVE else TaxMode.EXCLUSIVE,
        ),
        serviceCharge = ServiceChargeConfig(serviceChargeBasisPoints, serviceChargeTaxable),
        rounding = roundingRule,
    )
}

interface StoreProfileRepository {
    fun observeProfile(): Flow<StoreProfile?>

    suspend fun getProfile(): StoreProfile?

    suspend fun upsertProfile(profile: StoreProfile)
}
