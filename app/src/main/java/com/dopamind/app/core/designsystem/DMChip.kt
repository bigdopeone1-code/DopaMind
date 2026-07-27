package com.dopamind.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.BorderSubtle
import com.dopamind.app.core.theme.ChipShape
import com.dopamind.app.core.theme.TextSecondary

/**
 * A single-tap toggle chip — the building block of the Daily Vibe Check-in's
 * "no numeric input" consumption log.
 */
@Composable
fun DMChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = label,
        color = if (selected) Accent else TextSecondary,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .clip(ChipShape)
            .background(if (selected) Accent.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (selected) Accent else BorderSubtle, ChipShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
