package com.leanecorps.dapurjember.feature.floor.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.leanecorps.dapurjember.feature.floor.FloorScreen

// TODO: type-safe routes once kotlinx-serialization is added (arch §2).
const val FLOOR_ROUTE = "floor"

@Suppress("LongParameterList") // the home screen fans out to every top-level destination
fun NavGraphBuilder.floorScreen(
    onOpenTable: (tableId: String) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable(route = FLOOR_ROUTE) {
        FloorScreen(
            onOpenTable = onOpenTable,
            onOpenMenu = onOpenMenu,
            onOpenReports = onOpenReports,
            onOpenInventory = onOpenInventory,
            onOpenSettings = onOpenSettings,
        )
    }
}
