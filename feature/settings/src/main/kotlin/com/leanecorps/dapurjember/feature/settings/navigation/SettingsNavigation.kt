package com.leanecorps.dapurjember.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.leanecorps.dapurjember.feature.settings.backup.BackupScreen
import com.leanecorps.dapurjember.feature.settings.printers.PrintersScreen
import com.leanecorps.dapurjember.feature.settings.setup.SetupWizardScreen
import com.leanecorps.dapurjember.feature.settings.staff.StaffScreen

// TODO: type-safe routes once kotlinx-serialization is added (arch §2).
const val SETUP_ROUTE = "setup"
const val PRINTERS_ROUTE = "settings/printers"
const val BACKUP_ROUTE = "settings/backup"
const val STAFF_ROUTE = "settings/staff"

fun NavController.navigateToPrinters() = navigate(PRINTERS_ROUTE)

fun NavController.navigateToBackup() = navigate(BACKUP_ROUTE)

fun NavController.navigateToStaff() = navigate(STAFF_ROUTE)

fun NavGraphBuilder.setupWizardScreen(onComplete: () -> Unit) {
    composable(SETUP_ROUTE) {
        SetupWizardScreen(onComplete = onComplete)
    }
}

fun NavGraphBuilder.printersScreen(
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenStaff: () -> Unit,
) {
    composable(PRINTERS_ROUTE) {
        PrintersScreen(onBack = onBack, onOpenBackup = onOpenBackup, onOpenStaff = onOpenStaff)
    }
}

fun NavGraphBuilder.backupScreen(onBack: () -> Unit) {
    composable(BACKUP_ROUTE) {
        BackupScreen(onBack = onBack)
    }
}

fun NavGraphBuilder.staffScreen(onBack: () -> Unit) {
    composable(STAFF_ROUTE) {
        StaffScreen(onBack = onBack)
    }
}
