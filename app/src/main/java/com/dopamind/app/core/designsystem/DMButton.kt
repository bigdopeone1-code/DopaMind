package com.dopamind.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.BackgroundPrimary
import com.dopamind.app.core.theme.BorderHover
import com.dopamind.app.core.theme.ButtonShape
import com.dopamind.app.core.theme.TextPrimary

@Composable
fun DMPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = text,
        color = BackgroundPrimary,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
            .fillMaxWidth()
            .clip(ButtonShape)
            .background(Accent)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = 14.dp),
    )
}

@Composable
fun DMSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = text,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
            .fillMaxWidth()
            .clip(ButtonShape)
            .border(1.dp, BorderHover, ButtonShape)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = 14.dp),
    )
}
