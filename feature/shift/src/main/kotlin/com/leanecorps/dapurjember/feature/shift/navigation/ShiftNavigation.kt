package com.leanecorps.dapurjember.feature.shift.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.leanecorps.dapurjember.feature.shift.ShiftScreen

// TODO: type-safe routes once kotlinx-serialization is added (arch §2).
const val SHIFT_ROUTE = "shift"

fun NavGraphBuilder.shiftScreen(onDone: () -> Unit) {
    composable(route = SHIFT_ROUTE) { ShiftScreen(onDone = onDone) }
}
