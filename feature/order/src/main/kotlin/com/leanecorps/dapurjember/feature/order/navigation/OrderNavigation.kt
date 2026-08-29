package com.leanecorps.dapurjember.feature.order.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.leanecorps.dapurjember.feature.order.ORDER_ID_ARG
import com.leanecorps.dapurjember.feature.order.OrderScreen

// TODO: type-safe routes once kotlinx-serialization is added (arch §2).
const val ORDER_ROUTE = "order"

fun NavController.navigateToOrder(orderId: String) = navigate("$ORDER_ROUTE/$orderId")

fun NavGraphBuilder.orderScreen(onCheckout: (orderId: String) -> Unit) {
    composable(
        route = "$ORDER_ROUTE/{$ORDER_ID_ARG}",
        arguments = listOf(navArgument(ORDER_ID_ARG) { type = NavType.StringType }),
    ) {
        OrderScreen(onCheckout = onCheckout)
    }
}
