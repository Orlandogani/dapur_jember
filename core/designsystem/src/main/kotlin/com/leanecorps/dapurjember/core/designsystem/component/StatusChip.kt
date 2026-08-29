package com.leanecorps.dapurjember.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * A small filled pill for table / order state. Colour carries the meaning
 * (`docs/4-design` §6) — pass one of the `LocalPosStatusColors` values.
 */
@Composable
fun StatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val onColor = if (color.luminance() > 0.5f) Color.Black else Color.White
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = onColor,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
