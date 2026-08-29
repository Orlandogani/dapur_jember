package com.leanecorps.dapurjember.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Status colours with fixed meaning across light and dark (`docs/4-design` §6):
 * green = free/paid, amber = attention/low stock, red = void/variance, blue = occupied.
 * Access via `MaterialTheme` is not possible for these, so they ride a CompositionLocal.
 */
@Immutable
data class PosStatusColors(
    val free: Color = PosGreen,
    val paid: Color = PosGreen,
    val attention: Color = PosAmber,
    val lowStock: Color = PosAmber,
    val voided: Color = PosRed,
    val variance: Color = PosRed,
    val occupied: Color = PosBlue,
    val needsCleaning: Color = PosGrey,
)

val LocalPosStatusColors = staticCompositionLocalOf { PosStatusColors() }
