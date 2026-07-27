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
import androidx.compose.ui.unit.dp
import com.dopamind.app.core.theme.BackgroundElevated
import com.dopamind.app.core.theme.BorderSubtle
import com.dopamind.app.core.theme.CardShape

/**
 * The base surface used across every module: elevated background, a single
 * subtle 1px border, 16dp radius, and deliberately no shadow/glow per the
 * design system.
 */
@Composable
fun DMCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = CardShape,
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
            .clip(shape)
            .background(BackgroundElevated)
            .border(1.dp, BorderSubtle, shape)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content,
    )
}
