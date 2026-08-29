package com.leanecorps.dapurjember.feature.settings.setup

import com.leanecorps.dapurjember.core.domain.config.SetupParams
import com.leanecorps.dapurjember.core.domain.pricing.RoundingRule

/** A currency the wizard offers, with its conventional minor-unit scale and rounding. */
enum class CurrencyChoice(
    val code: String,
    val minorUnits: Int,
    val defaultRounding: RoundingRule,
) {
    IDR("IDR", 0, RoundingRule.NEAREST_100),
    MYR("MYR", 2, RoundingRule.NEAREST_5),
    USD("USD", 2, RoundingRule.NONE),
    SGD("SGD", 2, RoundingRule.NONE),
    PHP("PHP", 2, RoundingRule.NONE),
    THB("THB", 2, RoundingRule.NONE),
}

enum class SetupStep { BUSINESS, TAX, OWNER }

private const val PIN_MIN = 4
private const val PIN_MAX = 6
private const val MAX_PERCENT = 100.0
private const val BASIS_POINTS_PER_PERCENT = 100

data class SetupWizardUiState(
    val step: SetupStep = SetupStep.BUSINESS,
    val restaurantName: String = "",
    val currency: CurrencyChoice = CurrencyChoice.IDR,
    val timezoneId: String = "Asia/Jakarta",
    val taxPercentText: String = "0",
    val taxInclusive: Boolean = true,
    val serviceChargePercentText: String = "0",
    val serviceChargeTaxable: Boolean = false,
    val ownerName: String = "",
    val ownerPin: String = "",
    val saving: Boolean = false,
    val done: Boolean = false,
) {
    val taxPercent: Double? get() = taxPercentText.toPercentOrNull()
    val serviceChargePercent: Double? get() = serviceChargePercentText.toPercentOrNull()

    val canContinueBusiness: Boolean get() = restaurantName.isNotBlank() && timezoneId.isNotBlank()
    val canContinueTax: Boolean get() = taxPercent != null && serviceChargePercent != null
    val canFinish: Boolean
        get() = !saving && ownerName.isNotBlank() && ownerPin.length in PIN_MIN..PIN_MAX

    fun toParams(): SetupParams = SetupParams(
        restaurantName = restaurantName.trim(),
        currencyCode = currency.code,
        currencyMinorUnits = currency.minorUnits,
        taxRateBasisPoints = ((taxPercent ?: 0.0) * BASIS_POINTS_PER_PERCENT).toInt(),
        taxInclusive = taxInclusive,
        serviceChargeBasisPoints = ((serviceChargePercent ?: 0.0) * BASIS_POINTS_PER_PERCENT).toInt(),
        serviceChargeTaxable = serviceChargeTaxable,
        roundingRule = currency.defaultRounding,
        businessDayCutoffMinutes = DEFAULT_BUSINESS_DAY_CUTOFF_MIN,
        timezoneId = timezoneId.trim(),
        ownerName = ownerName.trim(),
        ownerPin = ownerPin,
    )

    companion object {
        const val DEFAULT_BUSINESS_DAY_CUTOFF_MIN = 240 // 4am — the flows doc's 23:55-order fix
        const val OWNER_PIN_MIN = PIN_MIN
        const val OWNER_PIN_MAX = PIN_MAX
    }
}

private fun String.toPercentOrNull(): Double? {
    val value = trim().replace(',', '.').toDoubleOrNull() ?: return null
    return value.takeIf { it in 0.0..MAX_PERCENT }
}
