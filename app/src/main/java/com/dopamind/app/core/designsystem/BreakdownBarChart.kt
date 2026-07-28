package com.dopamind.app.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dopamind.app.core.theme.BorderSubtle
import com.dopamind.app.core.theme.TextSecondary

data class BarValue(val value: Float, val color: Color)
data class BarGroup(val label: String, val bars: List<BarValue>)

/**
 * One or more colored bars per labeled group, clustered side-by-side —
 * covers both "one category per bar" (breakdown) and "N series per
 * category" (week-over-week) with the same shape. No external charting
 * library, consistent with SimpleBarChart's own scaling approach.
 */
@Composable
fun BreakdownBarChart(
    groups: List<BarGroup>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    trackColor: Color = BorderSubtle,
) {
    val maxValue = (groups.flatMap { it.bars }.maxOfOrNull { it.value } ?: 0f).coerceAtLeast(1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            drawLine(
                color = trackColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx(),
            )

            if (groups.isEmpty()) return@Canvas

            val groupGap = 12.dp.toPx()
            val barGap = 2.dp.toPx()
            val groupWidth = ((size.width - groupGap * (groups.size - 1)) / groups.size).coerceAtLeast(1f)

            groups.forEachIndexed { groupIndex, group ->
                val groupLeft = groupIndex * (groupWidth + groupGap)
                val barCount = group.bars.size.coerceAtLeast(1)
                val barWidth = ((groupWidth - barGap * (barCount - 1)) / barCount).coerceAtLeast(1f)

                group.bars.forEachIndexed { barIndex, bar ->
                    val barHeight = (bar.value / maxValue) * size.height
                    val left = groupLeft + barIndex * (barWidth + barGap)
                    drawRect(
                        color = if (bar.value > 0f) bar.color else trackColor,
                        topLeft = Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight.coerceAtLeast(2f)),
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            groups.forEach { group ->
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
