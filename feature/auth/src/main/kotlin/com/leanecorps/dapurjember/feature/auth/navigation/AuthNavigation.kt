package com.leanecorps.dapurjember.feature.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.leanecorps.dapurjember.feature.auth.PinLockScreen

// TODO: type-safe routes once kotlinx-serialization is added (arch §2).
const val PIN_LOCK_ROUTE = "pin-lock"

fun NavGraphBuilder.pinLockScreen(onSignedIn: () -> Unit) {
    composable(route = PIN_LOCK_ROUTE) { PinLockScreen(onSignedIn = onSignedIn) }
}
