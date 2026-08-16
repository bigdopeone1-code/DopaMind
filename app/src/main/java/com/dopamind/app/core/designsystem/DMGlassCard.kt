package com.dopamind.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dopamind.app.core.theme.BackgroundElevated
import com.dopamind.app.core.theme.CardShape

/**
 * A soft, translucent "glass" variant of DMCard — calm/premium style (subtle
 * white fill + hairline border + soft shadow), not a glow/neon effect. Same
 * ColumnScope contract as DMCard, so it's a drop-in swap at call sites.
 */
@Composable
fun DMGlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = CardShape,
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    borderWidth: Dp = 1.dp,
    surfaceColor: Color = BackgroundElevated,
    surfaceAlpha: Float = 0.9f,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.4f),
            )
            .clip(shape)
            .background(surfaceColor.copy(alpha = surfaceAlpha))
            .background(Color.White.copy(alpha = 0.05f))
            .border(borderWidth, borderColor, shape)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content,
    )
}
