package com.dopamind.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.BackgroundPrimary
import com.dopamind.app.core.theme.BorderSubtle
import com.dopamind.app.core.theme.ChipShape
import com.dopamind.app.core.theme.TextDisabled
import com.dopamind.app.core.theme.TextPrimary

/** A minimal single/multi-line text field matching the flat, borderless-input aesthetic. */
@Composable
fun DMTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
        modifier = modifier
            .fillMaxWidth()
            .clip(ChipShape)
            .background(BackgroundPrimary)
            .border(1.dp, BorderSubtle, ChipShape)
            .padding(12.dp),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = TextDisabled)
            }
            innerTextField()
        },
    )
}
