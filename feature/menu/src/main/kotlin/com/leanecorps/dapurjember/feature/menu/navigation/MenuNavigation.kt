package com.leanecorps.dapurjember.feature.menu.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.leanecorps.dapurjember.feature.menu.MenuScreen
import com.leanecorps.dapurjember.feature.menu.csv.ImportMenuScreen
import com.leanecorps.dapurjember.feature.menu.editor.MENU_ITEM_ID_ARG
import com.leanecorps.dapurjember.feature.menu.editor.MenuItemEditorScreen
import com.leanecorps.dapurjember.feature.menu.modifiers.ModifierGroupsScreen

// TODO: migrate to type-safe routes (@Serializable) once kotlinx-serialization is added (arch §2).
const val MENU_ROUTE = "menu"
private const val MENU_ITEM_EDITOR_ROUTE = "menu/item"
private const val MENU_CSV_ROUTE = "menu/import"
private const val MODIFIER_GROUPS_ROUTE = "menu/modifiers"

fun NavController.navigateToMenu(navOptions: NavOptions? = null) = navigate(MENU_ROUTE, navOptions)

fun NavController.navigateToMenuItemEditor(itemId: String?) =
    navigate(if (itemId == null) MENU_ITEM_EDITOR_ROUTE else "$MENU_ITEM_EDITOR_ROUTE?$MENU_ITEM_ID_ARG=$itemId")

fun NavController.navigateToMenuCsvImport() = navigate(MENU_CSV_ROUTE)

fun NavController.navigateToModifierGroups() = navigate(MODIFIER_GROUPS_ROUTE)

fun NavGraphBuilder.menuScreen(
    onEditItem: (itemId: String?) -> Unit,
    onImportCsv: () -> Unit,
    onModifierGroups: () -> Unit,
) {
    composable(route = MENU_ROUTE) {
        MenuScreen(onEditItem = onEditItem, onImportCsv = onImportCsv, onModifierGroups = onModifierGroups)
    }
}

fun NavGraphBuilder.menuCsvImportScreen(onBack: () -> Unit) {
    composable(route = MENU_CSV_ROUTE) { ImportMenuScreen(onBack = onBack) }
}

fun NavGraphBuilder.modifierGroupsScreen(onBack: () -> Unit) {
    composable(route = MODIFIER_GROUPS_ROUTE) { ModifierGroupsScreen(onBack = onBack) }
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
