package com.dopamind.app.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.BorderSubtle
import com.dopamind.app.core.theme.TextSecondary

/** One entry per discrete slider position, from lowest (0) to highest (steps-1). */
data class MoodStep(val emoji: String, val label: String)

/**
 * The single-gesture emoji slider used on card 1 of the Daily Vibe Check-in
 * (energy/mood). Emoji are deliberately used here — they're the copy, not a
 * UI icon.
 */
@Composable
fun EmojiMoodSlider(
    steps: List<MoodStep>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = steps[selectedIndex].emoji,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = steps[selectedIndex].label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
        )
        Slider(
            value = selectedIndex.toFloat(),
            onValueChange = { onSelectedIndexChange(it.toInt().coerceIn(0, steps.lastIndex)) },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = BorderSubtle,
            ),
        )
    }
}
