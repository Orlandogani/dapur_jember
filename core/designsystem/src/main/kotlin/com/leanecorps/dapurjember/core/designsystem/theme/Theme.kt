package com.leanecorps.dapurjember.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Neutral99,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Sand40,
    onSecondary = Neutral99,
    secondaryContainer = Sand90,
    onSecondaryContainer = Sand10,
    error = Error40,
    onError = Neutral99,
    errorContainer = Error80,
    onErrorContainer = Error10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    onSurfaceVariant = Neutral20,
    outline = PosGrey,
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal10,
    primaryContainer = Teal20,
    onPrimaryContainer = Teal90,
    secondary = Sand80,
    onSecondary = Sand10,
    secondaryContainer = Sand40,
    onSecondaryContainer = Sand90,
    error = Error80,
    onError = Error10,
    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = Neutral90,
    outline = PosGrey,
)

/** Minimum touch target on the order screen — larger than Material's 48dp (`docs/4-design` §6). */
val PosTouchTarget = 56.dp

/**
 * The app theme. High-contrast POS palette, dark mode always available, no dynamic colour
 * (colour is load-bearing on this UI). Exposes [LocalPosStatusColors] for status chips.
 */
@Composable
fun DapurJemberTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPosStatusColors provides PosStatusColors()) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            content = content,
        )
    }
}
