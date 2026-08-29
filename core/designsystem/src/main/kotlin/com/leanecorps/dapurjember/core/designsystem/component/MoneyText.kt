package com.leanecorps.dapurjember.core.designsystem.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.leanecorps.dapurjember.core.designsystem.theme.MoneyTextStyle

/**
 * Renders an already-formatted monetary amount (or quantity) with tabular figures so it
 * lines up in columns. Formatting is the caller's job (`formatMoney` in `:core:common`).
 */
@Composable
fun MoneyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(text = text, modifier = modifier, style = style.merge(MoneyTextStyle))
}
