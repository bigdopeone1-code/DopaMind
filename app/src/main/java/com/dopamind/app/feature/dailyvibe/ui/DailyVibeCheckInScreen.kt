package com.dopamind.app.feature.dailyvibe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMChip
import com.dopamind.app.core.designsystem.DMPrimaryButton
import com.dopamind.app.core.designsystem.DMTextField
import com.dopamind.app.core.designsystem.EmojiMoodSlider
import com.dopamind.app.core.designsystem.MoodStep
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.gamification.BadgeId
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.BorderSubtle
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.feature.dailyvibe.domain.CheckInReaction
import kotlin.math.roundToInt

private const val PAGE_COUNT = 3

@Composable
fun DailyVibeCheckInScreen(onFinish: () -> Unit, onBack: () -> Unit) {
    val viewModel = dopaMindViewModel { container ->
        DailyVibeCheckInViewModel(container.dailyVibeRepository, container.badgeEngine)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        val result = uiState.result
        if (result != null) {
            CheckInResultContent(result = result, onDone = onFinish)
        } else {
            CheckInPagerContent(
                uiState = uiState,
                onPageChange = viewModel::onPageChange,
                onMoodChange = viewModel::onMoodChange,
                onToggleModule = viewModel::onToggleModule,
                onSleepHoursChange = viewModel::onSleepHoursChange,
                onNoteChange = viewModel::onNoteChange,
                onSubmit = viewModel::submit,
            )
        }

        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close), tint = TextSecondary)
        }
    }
}

@Composable
private fun CheckInPagerContent(
    uiState: DailyVibeUiState,
    onPageChange: (Int) -> Unit,
    onMoodChange: (Int) -> Unit,
    onToggleModule: (ConsumedModuleChip) -> Unit,
    onSleepHoursChange: (Float) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 0) { PAGE_COUNT }

    Column(modifier = Modifier.fillMaxSize().padding(top = 56.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)) {
        PagerDots(pageCount = PAGE_COUNT, currentPage = pagerState.currentPage)
        Spacer(Modifier.height(24.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            when (page) {
                0 -> MoodCard(uiState.moodEnergyIndex, onMoodChange)
                1 -> ConsumptionChipsCard(uiState.selectedModules, onToggleModule)
                else -> SleepAndNoteCard(uiState.sleepHours, uiState.note, onSleepHoursChange, onNoteChange)
            }
        }

        Spacer(Modifier.height(16.dp))
        DMPrimaryButton(
            text = stringResource(if (pagerState.currentPage == PAGE_COUNT - 1) R.string.checkin_finish else R.string.checkin_next),
            onClick = {
                if (pagerState.currentPage == PAGE_COUNT - 1) {
                    onSubmit()
                } else {
                    val next = pagerState.currentPage + 1
                    onPageChange(next)
                }
            },
        )
    }
}

@Composable
private fun PagerDots(pageCount: Int, currentPage: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (index <= currentPage) Accent else BorderSubtle),
            )
        }
    }
}

@Composable
private fun MoodCard(selectedIndex: Int, onMoodChange: (Int) -> Unit) {
    val steps = listOf(
        MoodStep("😫", stringResource(R.string.checkin_mood_drained)),
        MoodStep("😐", stringResource(R.string.checkin_mood_low)),
        MoodStep("🙂", stringResource(R.string.checkin_mood_okay)),
        MoodStep("😄", stringResource(R.string.checkin_mood_good)),
        MoodStep("🚀", stringResource(R.string.checkin_mood_energized)),
    )
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.checkin_mood_title),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))
        EmojiMoodSlider(steps = steps, selectedIndex = selectedIndex, onSelectedIndexChange = onMoodChange)
    }
}

@Composable
private fun ConsumptionChipsCard(selected: Set<ConsumedModuleChip>, onToggle: (ConsumedModuleChip) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.checkin_chips_title),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.checkin_chips_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(120.dp),
        ) {
            items(chipLabelRes.entries.toList()) { (chip, labelRes) ->
                DMChip(
                    label = stringResource(labelRes),
                    selected = chip in selected,
                    onClick = { onToggle(chip) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private val chipLabelRes = mapOf(
    ConsumedModuleChip.CANNABIS to R.string.module_cannabis,
    ConsumedModuleChip.TOBACCO to R.string.module_tobacco,
    ConsumedModuleChip.ALCOHOL to R.string.module_alcohol,
    ConsumedModuleChip.LIBIDO to R.string.module_libido,
)

@Composable
private fun SleepAndNoteCard(sleepHours: Float, note: String, onSleepHoursChange: (Float) -> Unit, onNoteChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.checkin_sleep_title),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.checkin_sleep_hours_value, sleepHours.roundToInt()),
            style = MaterialTheme.typography.headlineLarge,
            color = Accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Slider(
            value = sleepHours,
            onValueChange = onSleepHoursChange,
            valueRange = 0f..12f,
            steps = 11,
            colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = BorderSubtle),
        )
        Spacer(Modifier.height(24.dp))
        DMTextField(
            value = note,
            onValueChange = onNoteChange,
            placeholder = stringResource(R.string.checkin_note_placeholder),
            minLines = 2,
        )
    }
}

@Composable
private fun CheckInResultContent(result: DailyVibeResult, onDone: () -> Unit) {
    val (emoji, titleRes) = reactionCopy(result.reaction)
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        if (result.streak > 1) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.checkin_streak_caption, result.streak),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
        if (result.newlyUnlockedBadges.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            result.newlyUnlockedBadges.forEach { badgeId ->
                Text(
                    text = stringResource(R.string.checkin_badge_unlocked, badgeTitle(badgeId)),
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        DMPrimaryButton(text = stringResource(R.string.checkin_done), onClick = onDone)
    }
}

@Composable
private fun reactionCopy(reaction: CheckInReaction): Pair<String, Int> = when (reaction) {
    CheckInReaction.WEEKEND_WARRIOR -> "🏆" to R.string.reaction_weekend_warrior
    CheckInReaction.CLEAN_DAY -> "✨" to R.string.reaction_clean_day
    CheckInReaction.NIGHT_OWL -> "🦉" to R.string.reaction_night_owl
    CheckInReaction.ENERGIZED -> "⚡" to R.string.reaction_energized
    CheckInReaction.LOW_BATTERY -> "🔋" to R.string.reaction_low_battery
    CheckInReaction.BALANCED -> "🧘" to R.string.reaction_balanced
}

@Composable
private fun badgeTitle(badgeId: BadgeId): String = stringResource(
    when (badgeId) {
        BadgeId.CHECKIN_STREAK_7 -> R.string.badge_checkin_streak_7
        BadgeId.CHECKIN_STREAK_30 -> R.string.badge_checkin_streak_30
        BadgeId.TBREAK_HERO_7 -> R.string.badge_tbreak_hero_7
        BadgeId.SMOKE_FREE_3 -> R.string.badge_smoke_free_3
        BadgeId.LIVER_FRIEND_7 -> R.string.badge_liver_friend_7
        BadgeId.MONK_MODE_7 -> R.string.badge_monk_mode_7
        BadgeId.DETOX_MASTER_3 -> R.string.badge_detox_master_3
        BadgeId.BUDGET_BOSS -> R.string.badge_budget_boss
    }
)
