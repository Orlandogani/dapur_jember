package com.leanecorps.dapurjember.feature.payment.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.leanecorps.dapurjember.feature.payment.PAYMENT_ORDER_ID_ARG
import com.leanecorps.dapurjember.feature.payment.PaymentScreen

// TODO: type-safe routes once kotlinx-serialization is added (arch §2).
const val PAYMENT_ROUTE = "payment"

fun NavController.navigateToPayment(orderId: String) = navigate("$PAYMENT_ROUTE/$orderId")

fun NavGraphBuilder.paymentScreen(onSettled: () -> Unit) {
    composable(
        route = "$PAYMENT_ROUTE/{$PAYMENT_ORDER_ID_ARG}",
        arguments = listOf(navArgument(PAYMENT_ORDER_ID_ARG) { type = NavType.StringType }),
    ) {
        PaymentScreen(onSettled = onSettled)
    }
}
