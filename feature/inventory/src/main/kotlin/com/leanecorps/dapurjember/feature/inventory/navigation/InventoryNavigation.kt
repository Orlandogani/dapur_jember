package com.leanecorps.dapurjember.feature.inventory.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.leanecorps.dapurjember.feature.inventory.InventoryScreen

// TODO: type-safe routes once kotlinx-serialization is added (arch §2).
const val INVENTORY_ROUTE = "inventory"

fun NavController.navigateToInventory() = navigate(INVENTORY_ROUTE)

fun NavGraphBuilder.inventoryScreen(onBack: () -> Unit) {
    composable(INVENTORY_ROUTE) { InventoryScreen(onBack = onBack) }
}
