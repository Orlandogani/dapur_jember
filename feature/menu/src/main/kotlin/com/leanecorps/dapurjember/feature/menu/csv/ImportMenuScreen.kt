@file:OptIn(ExperimentalMaterial3Api::class)

package com.leanecorps.dapurjember.feature.menu.csv

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton
import com.leanecorps.dapurjember.core.domain.menu.MenuCsvError
import com.leanecorps.dapurjember.core.domain.menu.MenuCsvErrorReason
import com.leanecorps.dapurjember.feature.menu.R

@Composable
fun ImportMenuScreen(
    onBack: () -> Unit,
    viewModel: ImportMenuViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.import_title)) }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.import_help),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = state.text,
                onValueChange = viewModel::setText,
                label = { Text(stringResource(R.string.import_csv_label)) },
                minLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PosOutlinedButton(
                    text = stringResource(R.string.import_action_back),
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                PosButton(
                    text = stringResource(
                        if (state.running) R.string.import_action_importing else R.string.import_action_import,
                    ),
                    onClick = viewModel::import,
                    enabled = !state.running && state.text.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
            state.summary?.let { summary ->
                Text(
                    stringResource(R.string.import_summary, summary.itemsImported, summary.categoriesAdded),
                    style = MaterialTheme.typography.bodyMedium,
                )
                summary.errors.forEach { error ->
                    Text(
                        stringResource(
                            R.string.import_error_line,
                            error.line,
                            stringResource(error.messageRes(), error.detail.orEmpty()),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/**
 * The importer reports a reason code; the sentence the user reads is assembled here so it
 * can be translated (NFR8). Every message takes the offending value as its one argument,
 * which the reasons that have no detail simply ignore.
 */
@StringRes
private fun MenuCsvError.messageRes(): Int = when (reason) {
    MenuCsvErrorReason.TOO_FEW_COLUMNS -> R.string.import_error_too_few_columns
    MenuCsvErrorReason.BLANK_CATEGORY -> R.string.import_error_blank_category
    MenuCsvErrorReason.BLANK_ITEM -> R.string.import_error_blank_item
    MenuCsvErrorReason.INVALID_PRICE -> R.string.import_error_invalid_price
}
