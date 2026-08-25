package com.dopamind.app.feature.log.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.DMChip
import com.dopamind.app.core.designsystem.DMPrimaryButton
import com.dopamind.app.core.designsystem.DMSecondaryButton
import com.dopamind.app.core.designsystem.DMTextField
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.habit.EventContext
import com.dopamind.app.core.habit.EventTrigger
import com.dopamind.app.core.habit.HabitCategory
import com.dopamind.app.core.theme.TextDisabled
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary

private val FIVE_POINT_SCALE = listOf(1, 2, 3, 4, 5)

@Composable
private fun whenLabel(option: WhenOption): String = stringResource(
    when (option) {
        WhenOption.NOW -> R.string.when_now
        WhenOption.FIFTEEN_MIN -> R.string.when_15_min
        WhenOption.ONE_HOUR -> R.string.when_1_hour
        WhenOption.THREE_HOURS -> R.string.when_3_hours
    }
)

/**
 * The central "+" flow.
 *
 * Structured as one scrolling screen rather than a wizard: the required part
 * (what, how much) sits at the top with Save immediately under it, and every
 * optional field lives below that. A user in a hurry taps three times and is
 * done; a user who wants to capture context keeps scrolling. Nothing below the
 * Save button can ever block a log from being written.
 */
@Composable
fun AddEventScreen(presetCategory: String?, onDone: () -> Unit) {
    val viewModel = dopaMindViewModel { container ->
        AddEventViewModel(
            behaviorEventRepository = container.behaviorEventRepository,
            profileRepository = container.profileRepository,
            presetCategoryName = presetCategory,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(titleRes = R.string.add_event_title, onBack = onDone) }

        val category = state.category
        if (category == null) {
            item { CategoryPicker(state, onSelect = viewModel::selectCategory, onToggleAll = viewModel::toggleShowAllCategories) }
            return@LazyColumn
        }

        item {
            AmountCard(
                category = category,
                amount = state.amount,
                onAdd = viewModel::addAmount,
                onSet = viewModel::setAmount,
                onChangeCategory = viewModel::clearCategory,
            )
        }

        item {
            DMPrimaryButton(
                text = stringResource(R.string.add_event_save),
                onClick = viewModel::save,
                enabled = state.canSave,
            )
        }

        item {
            Text(
                text = stringResource(R.string.add_event_optional_hint),
                style = MaterialTheme.typography.labelMedium,
                color = TextDisabled,
            )
        }

        item { WhenCard(state.whenOption, viewModel::setWhen) }
        item {
            ScaleCard(
                titleRes = R.string.add_event_intensity,
                value = state.intensity,
                onSelect = viewModel::setIntensity,
            )
        }
        item {
            ScaleCard(
                titleRes = R.string.add_event_mood,
                value = state.mood,
                onSelect = viewModel::setMood,
            )
        }
        item {
            ScaleCard(
                titleRes = R.string.add_event_stress,
                value = state.stress,
                onSelect = viewModel::setStress,
            )
        }
        item { TriggerCard(state.trigger, viewModel::setTrigger) }
        item { ContextCard(state.context, viewModel::setContext) }
        item { NoteCard(state.note, viewModel::setNote) }
    }
}

@Composable
private fun CategoryPicker(
    state: AddEventUiState,
    onSelect: (HabitCategory) -> Unit,
    onToggleAll: () -> Unit,
) {
    // The user's own categories come first and are usually the whole list they
    // need; everything else stays one tap away rather than cluttering the sheet.
    val shown = if (state.showAllCategories) HabitCategory.entries.toList() else state.trackedCategories

    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.add_event_what_happened),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )
        Spacer(Modifier.height(12.dp))
        shown.forEach { category ->
            DMSecondaryButton(
                text = stringResource(category.labelRes),
                onClick = { onSelect(category) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(4.dp))
        DMChip(
            label = stringResource(
                if (state.showAllCategories) R.string.add_event_show_mine else R.string.add_event_show_all
            ),
            selected = state.showAllCategories,
            onClick = onToggleAll,
        )
    }
}

@Composable
private fun AmountCard(
    category: HabitCategory,
    amount: Float,
    onAdd: (Int) -> Unit,
    onSet: (Float) -> Unit,
    onChangeCategory: () -> Unit,
) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(category.labelRes),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            DMChip(label = stringResource(R.string.add_event_change), selected = false, onClick = onChangeCategory)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "${amount.toInt()} ${stringResource(category.unitRes)}",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(category.quickAmounts) { quick ->
                DMChip(label = "+$quick", selected = false, onClick = { onAdd(quick) })
            }
            item {
                DMChip(label = stringResource(R.string.add_event_reset), selected = false, onClick = { onSet(0f) })
            }
        }
    }
}

@Composable
private fun WhenCard(selected: WhenOption, onSelect: (WhenOption) -> Unit) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.add_event_when), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(WhenOption.entries.toList()) { option ->
                DMChip(label = whenLabel(option), selected = selected == option, onClick = { onSelect(option) })
            }
        }
    }
}

@Composable
private fun ScaleCard(titleRes: Int, value: Int?, onSelect: (Int) -> Unit) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FIVE_POINT_SCALE.forEach { step ->
                DMChip(label = "$step", selected = value == step, onClick = { onSelect(step) })
            }
        }
    }
}

@Composable
private fun TriggerCard(selected: EventTrigger?, onSelect: (EventTrigger) -> Unit) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.add_event_trigger), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(EventTrigger.entries.toList()) { trigger ->
                DMChip(
                    label = stringResource(trigger.labelRes),
                    selected = selected == trigger,
                    onClick = { onSelect(trigger) },
                )
            }
        }
    }
}

@Composable
private fun ContextCard(selected: EventContext?, onSelect: (EventContext) -> Unit) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.add_event_context), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(EventContext.entries.toList()) { context ->
                DMChip(
                    label = stringResource(context.labelRes),
                    selected = selected == context,
                    onClick = { onSelect(context) },
                )
            }
        }
    }
}

@Composable
private fun NoteCard(note: String, onNoteChange: (String) -> Unit) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.add_event_note), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        DMTextField(
            value = note,
            onValueChange = onNoteChange,
            placeholder = stringResource(R.string.add_event_note_placeholder),
            singleLine = false,
        )
    }
}
