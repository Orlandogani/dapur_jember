package com.leanecorps.dapurjember.feature.menu.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.leanecorps.dapurjember.feature.menu.MenuScreen
import com.leanecorps.dapurjember.feature.menu.editor.MENU_ITEM_ID_ARG
import com.leanecorps.dapurjember.feature.menu.editor.MenuItemEditorScreen

// TODO: migrate to type-safe routes (@Serializable) once kotlinx-serialization is added (arch §2).
const val MENU_ROUTE = "menu"
private const val MENU_ITEM_EDITOR_ROUTE = "menu/item"

fun NavController.navigateToMenu(navOptions: NavOptions? = null) = navigate(MENU_ROUTE, navOptions)

fun NavController.navigateToMenuItemEditor(itemId: String?) =
    navigate(if (itemId == null) MENU_ITEM_EDITOR_ROUTE else "$MENU_ITEM_EDITOR_ROUTE?$MENU_ITEM_ID_ARG=$itemId")

fun NavGraphBuilder.menuScreen(onEditItem: (itemId: String?) -> Unit) {
    composable(route = MENU_ROUTE) { MenuScreen(onEditItem = onEditItem) }
}

fun NavGraphBuilder.menuItemEditorScreen(onDone: () -> Unit) {
    composable(
        route = "$MENU_ITEM_EDITOR_ROUTE?$MENU_ITEM_ID_ARG={$MENU_ITEM_ID_ARG}",
        arguments = listOf(
            navArgument(MENU_ITEM_ID_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) {
        MenuItemEditorScreen(onDone = onDone)
    }
}
