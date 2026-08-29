package com.leanecorps.dapurjember.feature.settings.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanecorps.dapurjember.core.domain.config.CompleteSetupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    private val completeSetup: CompleteSetupUseCase,
) : ViewModel(), SetupWizardActions {

    private val state = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = state.asStateFlow()

    override fun setRestaurantName(value: String) = state.update { it.copy(restaurantName = value) }

    override fun setCurrency(choice: CurrencyChoice) = state.update { it.copy(currency = choice) }

    override fun setTimezone(value: String) = state.update { it.copy(timezoneId = value) }

    override fun setTaxPercent(value: String) = state.update { it.copy(taxPercentText = value) }

    override fun setTaxInclusive(value: Boolean) = state.update { it.copy(taxInclusive = value) }

    override fun setServiceChargePercent(value: String) =
        state.update { it.copy(serviceChargePercentText = value) }

    override fun setServiceChargeTaxable(value: Boolean) = state.update { it.copy(serviceChargeTaxable = value) }

    override fun setOwnerName(value: String) = state.update { it.copy(ownerName = value) }

    override fun setOwnerPin(value: String) = state.update {
        it.copy(ownerPin = value.filter(Char::isDigit).take(SetupWizardUiState.OWNER_PIN_MAX))
    }

    override fun back() = state.update { current ->
        val steps = SetupStep.entries
        current.copy(step = steps[(steps.indexOf(current.step) - 1).coerceAtLeast(0)])
    }

    override fun next() = state.update { current ->
        val steps = SetupStep.entries
        current.copy(step = steps[(steps.indexOf(current.step) + 1).coerceAtMost(steps.lastIndex)])
    }

    override fun finish() {
        val snapshot = state.value
        if (!snapshot.canFinish) return
        state.update { it.copy(saving = true) }
        viewModelScope.launch {
            completeSetup(snapshot.toParams())
            state.update { it.copy(saving = false, done = true) }
        }
    }
}
