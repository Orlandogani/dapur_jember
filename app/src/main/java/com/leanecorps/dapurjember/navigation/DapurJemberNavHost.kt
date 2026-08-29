package com.leanecorps.dapurjember.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.leanecorps.dapurjember.feature.auth.navigation.PIN_LOCK_ROUTE
import com.leanecorps.dapurjember.feature.auth.navigation.pinLockScreen
import com.leanecorps.dapurjember.feature.floor.navigation.FLOOR_ROUTE
import com.leanecorps.dapurjember.feature.floor.navigation.floorScreen
import com.leanecorps.dapurjember.feature.menu.navigation.MENU_ROUTE
import com.leanecorps.dapurjember.feature.menu.navigation.menuCsvImportScreen
import com.leanecorps.dapurjember.feature.menu.navigation.menuItemEditorScreen
import com.leanecorps.dapurjember.feature.menu.navigation.menuScreen
import com.leanecorps.dapurjember.feature.menu.navigation.navigateToMenuCsvImport
import com.leanecorps.dapurjember.feature.menu.navigation.navigateToMenuItemEditor
import com.leanecorps.dapurjember.feature.order.navigation.navigateToOrder
import com.leanecorps.dapurjember.feature.order.navigation.orderScreen
import com.leanecorps.dapurjember.feature.payment.navigation.navigateToPayment
import com.leanecorps.dapurjember.feature.payment.navigation.paymentScreen
import com.leanecorps.dapurjember.feature.settings.navigation.SETUP_ROUTE
import com.leanecorps.dapurjember.feature.settings.navigation.navigateToPrinters
import com.leanecorps.dapurjember.feature.settings.navigation.printersScreen
import com.leanecorps.dapurjember.feature.settings.navigation.setupWizardScreen
import com.leanecorps.dapurjember.feature.shift.navigation.SHIFT_ROUTE
import com.leanecorps.dapurjember.feature.shift.navigation.shiftScreen
import kotlinx.coroutines.launch

private const val DEFAULT_GUESTS = 2

@Composable
fun DapurJemberNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: AppNavHostViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val hasOpenShift by viewModel.hasOpenShift.collectAsStateWithLifecycle()

    val start = lockedStartRoute(viewModel)
    if (start == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    NavHost(navController = navController, startDestination = start, modifier = modifier) {
        setupWizardScreen(
            onComplete = {
                navController.navigate(PIN_LOCK_ROUTE) {
                    popUpTo(SETUP_ROUTE) { inclusive = true }
                }
            },
        )
        pinLockScreen(
            onSignedIn = {
                val target = if (hasOpenShift) FLOOR_ROUTE else SHIFT_ROUTE
                navController.navigate(target) {
                    popUpTo(PIN_LOCK_ROUTE) { inclusive = true }
                }
            },
        )
        shiftScreen(
            onDone = {
                navController.navigate(FLOOR_ROUTE) {
                    popUpTo(SHIFT_ROUTE) { inclusive = true }
                }
            },
        )
        floorScreen(
            onOpenTable = { tableId ->
                scope.launch {
                    runCatching { viewModel.orderIdForTable(tableId, DEFAULT_GUESTS) }
                        .onSuccess { navController.navigateToOrder(it) }
                }
            },
            onOpenMenu = { navController.navigate(MENU_ROUTE) },
            onOpenSettings = { navController.navigateToPrinters() },
        )
        orderScreen(
            onCheckout = { orderId -> navController.navigateToPayment(orderId) },
        )
        paymentScreen(
            onSettled = {
                navController.navigate(FLOOR_ROUTE) {
                    popUpTo(FLOOR_ROUTE) { inclusive = true }
                }
            },
        )
        printersScreen(onBack = { navController.popBackStack() })
        menuScreen(
            onEditItem = { itemId -> navController.navigateToMenuItemEditor(itemId) },
            onImportCsv = { navController.navigateToMenuCsvImport() },
        )
        menuItemEditorScreen(onDone = { navController.popBackStack() })
        menuCsvImportScreen(onBack = { navController.popBackStack() })
    }
}

/**
 * The first non-null start-route decision, held stable so a later `store_profile` change
 * (e.g. finishing the setup wizard) can't swap the NavHost's graph root out from under it.
 * Returns null while the DB is still being read.
 */
@Composable
private fun lockedStartRoute(viewModel: AppNavHostViewModel): String? {
    val decided by viewModel.startRoute.collectAsStateWithLifecycle()
    var locked by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(decided) {
        if (locked == null) locked = decided
    }
    return locked
}
