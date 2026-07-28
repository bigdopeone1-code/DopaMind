package com.dopamind.app.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class PresetOption(val id: String, val label: String)

/**
 * Generic horizontally-scrollable "pick one" row that replaces free-text
 * input across Why Prompt, Health Stacking, Munchies category, and Cannabis
 * strain/note. Convention: options[0] is always the pre-selected default —
 * for optional fields, put a "None" option first; for required fields, put
 * the safest real default first. This guarantees there is never an
 * "unselected" state to handle.
 */
@Composable
fun QuickPresetChipRow(
    options: List<PresetOption>,
    selectedId: String,
    onSelect: (PresetOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options, key = { it.id }) { option ->
            DMChip(label = option.label, selected = option.id == selectedId, onClick = { onSelect(option) })
        }
    }
}
