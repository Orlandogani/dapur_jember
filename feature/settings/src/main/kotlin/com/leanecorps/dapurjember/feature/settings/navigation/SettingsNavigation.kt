package com.leanecorps.dapurjember.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.leanecorps.dapurjember.feature.settings.setup.SetupWizardScreen

// TODO: type-safe routes once kotlinx-serialization is added (arch §2).
const val SETUP_ROUTE = "setup"

fun NavGraphBuilder.setupWizardScreen(onComplete: () -> Unit) {
    composable(SETUP_ROUTE) {
        SetupWizardScreen(onComplete = onComplete)
    }
}
