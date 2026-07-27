package com.dopamind.app.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.BorderSubtle

/**
 * A minimal Canvas-drawn bar chart — no external charting library, kept
 * deliberately simple: one bar per value, scaled to the series' own max.
 */
@Composable
fun SimpleBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    barColor: androidx.compose.ui.graphics.Color = Accent,
    trackColor: androidx.compose.ui.graphics.Color = BorderSubtle,
    height: androidx.compose.ui.unit.Dp = 120.dp,
) {
    val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        // Baseline
        drawLine(
            color = trackColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )

        if (values.isEmpty()) return@Canvas

        val barGap = 3.dp.toPx()
        val barWidth = ((size.width - barGap * (values.size - 1)) / values.size).coerceAtLeast(1f)

        values.forEachIndexed { index, value ->
            val barHeight = (value / maxValue) * size.height
            val left = index * (barWidth + barGap)
            drawRect(
                color = if (value > 0f) barColor else trackColor,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight.coerceAtLeast(2f)),
            )
        }
    }
}
