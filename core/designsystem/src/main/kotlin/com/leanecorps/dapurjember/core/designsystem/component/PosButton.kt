package com.leanecorps.dapurjember.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.leanecorps.dapurjember.core.designsystem.theme.PosTouchTarget

/**
 * A primary action button sized for the order screen — at least [PosTouchTarget] tall
 * (56dp, above Material's 48dp) for greasy fingers on a busy Friday (`docs/4-design` §6).
 */
@Composable
fun PosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = PosTouchTarget),
        enabled = enabled,
    ) {
        Text(text)
    }
}

@Composable
fun PosOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = PosTouchTarget),
        enabled = enabled,
    ) {
        Text(text)
    }
}
