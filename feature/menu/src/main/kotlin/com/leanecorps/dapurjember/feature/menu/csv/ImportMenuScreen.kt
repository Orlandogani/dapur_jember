@file:OptIn(ExperimentalMaterial3Api::class)

package com.leanecorps.dapurjember.feature.menu.csv

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanecorps.dapurjember.core.designsystem.component.PosButton
import com.leanecorps.dapurjember.core.designsystem.component.PosOutlinedButton

@Composable
fun ImportMenuScreen(
    onBack: () -> Unit,
    viewModel: ImportMenuViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Import menu CSV") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "One row per variant: category, item, variant, price[, available]. " +
                    "A header row is optional; a blank variant becomes \"Regular\".",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = state.text,
                onValueChange = viewModel::setText,
                label = { Text("CSV") },
                minLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PosOutlinedButton(text = "Back", onClick = onBack, modifier = Modifier.weight(1f))
                PosButton(
                    text = if (state.running) "Importing…" else "Import",
                    onClick = viewModel::import,
                    enabled = !state.running && state.text.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
            state.summary?.let { summary ->
                Text(
                    "Imported ${summary.itemsImported} items, added ${summary.categoriesAdded} categories.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                summary.errors.forEach { error ->
                    Text(
                        "Line ${error.line}: ${error.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
