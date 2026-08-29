package com.leanecorps.dapurjember.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Default = FontFamily.Default

/**
 * Typography for the POS. Prices and counts must use **tabular figures** so columns of
 * numbers align (`docs/4-design` §6) — [MoneyTextStyle] applies `"tnum"`.
 */
val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

/** Base style for any on-screen monetary amount or quantity. Tabular figures, medium weight. */
val MoneyTextStyle: TextStyle = TextStyle(
    fontFamily = Default,
    fontWeight = FontWeight.Medium,
    fontFeatureSettings = "tnum",
)
