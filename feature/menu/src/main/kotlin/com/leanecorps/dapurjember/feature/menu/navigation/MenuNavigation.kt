package com.leanecorps.dapurjember.feature.menu.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.leanecorps.dapurjember.feature.menu.MenuScreen

// TODO: migrate to type-safe routes (@Serializable) once kotlinx-serialization is added (arch §2).
const val MENU_ROUTE = "menu"

fun NavController.navigateToMenu(navOptions: NavOptions? = null) = navigate(MENU_ROUTE, navOptions)

fun NavGraphBuilder.menuScreen() {
    composable(route = MENU_ROUTE) { MenuScreen() }
}
