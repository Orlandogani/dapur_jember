package com.leanecorps.dapurjember.feature.floor.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.leanecorps.dapurjember.feature.floor.FloorScreen

// TODO: type-safe routes once kotlinx-serialization is added (arch §2).
const val FLOOR_ROUTE = "floor"

fun NavGraphBuilder.floorScreen(
    onOpenTable: (tableId: String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable(route = FLOOR_ROUTE) {
        FloorScreen(onOpenTable = onOpenTable, onOpenSettings = onOpenSettings)
    }
}
