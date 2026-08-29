package com.leanecorps.dapurjember.feature.reports.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.leanecorps.dapurjember.feature.reports.ReportsScreen

// TODO: type-safe routes once kotlinx-serialization is added (arch §2).
const val REPORTS_ROUTE = "reports"

fun NavController.navigateToReports() = navigate(REPORTS_ROUTE)

fun NavGraphBuilder.reportsScreen(onBack: () -> Unit) {
    composable(REPORTS_ROUTE) { ReportsScreen(onBack = onBack) }
}
