package com.leanecorps.dapurjember.feature.settings.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.feature.settings.R

@Composable
fun SetupWizardScreen(
    onComplete: () -> Unit,
    viewModel: SetupWizardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.done) onComplete()
    SetupWizardScreen(state = state, actions = viewModel)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SetupWizardScreen(state: SetupWizardUiState, actions: SetupWizardActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
            .widthIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(
                R.string.setup_title,
                state.restaurantName.ifBlank { stringResource(R.string.setup_title_fallback) },
            ),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            stringResource(
                R.string.setup_step,
                SetupStep.entries.indexOf(state.step) + 1,
                SetupStep.entries.size,
            ),
            style = MaterialTheme.typography.labelLarge,
        )

        when (state.step) {
            SetupStep.BUSINESS -> BusinessStep(state, actions)
            SetupStep.TAX -> TaxStep(state, actions)
            SetupStep.OWNER -> OwnerStep(state, actions)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.step != SetupStep.BUSINESS) {
                PosOutlinedButton(
                    text = stringResource(R.string.setup_action_back),
                    onClick = actions::back,
                    modifier = Modifier.weight(1f),
                )
            }
            when (state.step) {
                SetupStep.BUSINESS -> PosButton(
                    text = stringResource(R.string.setup_action_next),
                    onClick = actions::next,
                    enabled = state.canContinueBusiness,
                    modifier = Modifier.weight(1f),
                )

                SetupStep.TAX -> PosButton(
                    text = stringResource(R.string.setup_action_next),
                    onClick = actions::next,
                    enabled = state.canContinueTax,
                    modifier = Modifier.weight(1f),
                )

                SetupStep.OWNER -> PosButton(
                    text = stringResource(
                        if (state.saving) R.string.setup_action_saving else R.string.setup_action_finish,
                    ),
                    onClick = actions::finish,
                    enabled = state.canFinish,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BusinessStep(state: SetupWizardUiState, actions: SetupWizardActions) {
    OutlinedTextField(
        value = state.restaurantName,
        onValueChange = actions::setRestaurantName,
        label = { Text(stringResource(R.string.setup_restaurant_name_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(R.string.setup_currency_heading), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CurrencyChoice.entries.forEach { choice ->
            FilterChip(
                selected = state.currency == choice,
                onClick = { actions.setCurrency(choice) },
                label = { Text(choice.code) },
            )
        }
    }
    OutlinedTextField(
        value = state.timezoneId,
        onValueChange = actions::setTimezone,
        label = { Text(stringResource(R.string.setup_timezone_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    AssistChip(onClick = {}, label = { Text(stringResource(R.string.setup_price_hint, state.currency.code)) })
}

@Composable
private fun TaxStep(state: SetupWizardUiState, actions: SetupWizardActions) {
    OutlinedTextField(
        value = state.taxPercentText,
        onValueChange = actions::setTaxPercent,
        label = { Text(stringResource(R.string.setup_tax_rate_label)) },
        isError = state.taxPercent == null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
    ToggleRow(stringResource(R.string.setup_tax_inclusive), state.taxInclusive, actions::setTaxInclusive)
    OutlinedTextField(
        value = state.serviceChargePercentText,
        onValueChange = actions::setServiceChargePercent,
        label = { Text(stringResource(R.string.setup_service_charge_label)) },
        isError = state.serviceChargePercent == null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
    ToggleRow(
        stringResource(R.string.setup_service_charge_taxable),
        state.serviceChargeTaxable,
        actions::setServiceChargeTaxable,
    )
}

@Composable
private fun OwnerStep(state: SetupWizardUiState, actions: SetupWizardActions) {
    OutlinedTextField(
        value = state.ownerName,
        onValueChange = actions::setOwnerName,
        label = { Text(stringResource(R.string.setup_owner_name_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.ownerPin,
        onValueChange = actions::setOwnerPin,
        label = {
            Text(
                stringResource(
                    R.string.setup_owner_pin_label,
                    SetupWizardUiState.OWNER_PIN_MIN,
                    SetupWizardUiState.OWNER_PIN_MAX,
                ),
            )
        },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        stringResource(R.string.setup_pin_warning),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** The subset of [SetupWizardViewModel] the stateless screen needs — keeps previews trivial. */
interface SetupWizardActions {
    fun setRestaurantName(value: String)
    fun setCurrency(choice: CurrencyChoice)
    fun setTimezone(value: String)
    fun setTaxPercent(value: String)
    fun setTaxInclusive(value: Boolean)
    fun setServiceChargePercent(value: String)
    fun setServiceChargeTaxable(value: Boolean)
    fun setOwnerName(value: String)
    fun setOwnerPin(value: String)
    fun back()
    fun next()
    fun finish()
}
