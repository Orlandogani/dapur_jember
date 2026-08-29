package com.leanecorps.dapurjember.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * POS palette (`docs/4-design` §6): high contrast, readable under fluorescent light and
 * daylight glare. Not a brand palette — rename once branding is decided.
 */

// Brand / primary — a deep teal, kitchen-adjacent and distinct from the semantic green.
internal val Teal10 = Color(0xFF00201A)
internal val Teal20 = Color(0xFF00382E)
internal val Teal40 = Color(0xFF006B58)
internal val Teal80 = Color(0xFF54DBBE)
internal val Teal90 = Color(0xFF74F8D9)

internal val Sand10 = Color(0xFF231A00)
internal val Sand40 = Color(0xFF6E5C00)
internal val Sand80 = Color(0xFFE6C200)
internal val Sand90 = Color(0xFFFFE264)

internal val Neutral6 = Color(0xFF101413)
internal val Neutral10 = Color(0xFF191C1B)
internal val Neutral20 = Color(0xFF2E3130)
internal val Neutral90 = Color(0xFFE1E3E1)
internal val Neutral95 = Color(0xFFEFF1EF)
internal val Neutral99 = Color(0xFFFBFDFB)

internal val Error10 = Color(0xFF410002)
internal val Error40 = Color(0xFFBA1A1A)
internal val Error80 = Color(0xFFFFB4AB)

// Semantic status colours — same meaning in both themes; used for chips, badges, variance.
val PosGreen = Color(0xFF2E7D32) // free / paid
val PosAmber = Color(0xFFF9A825) // attention / low stock
val PosRed = Color(0xFFC62828) // void / variance / negative
val PosBlue = Color(0xFF1565C0) // occupied
val PosGrey = Color(0xFF757575) // needs cleaning / inactive
