package com.dopamind.app.feature.badges.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.gamification.BadgeId
import com.dopamind.app.core.gamification.GamificationRepository
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.TextDisabled
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BadgeGalleryViewModel(repository: GamificationRepository) : ViewModel() {
    val unlockedIds: StateFlow<Set<String>> = repository.observeUnlocks()
        .map { unlocks -> unlocks.map { it.badgeId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
}

@Composable
fun BadgeGalleryScreen(onBack: () -> Unit) {
    val viewModel = dopaMindViewModel { container -> BadgeGalleryViewModel(container.gamificationRepository) }
    val unlockedIds by viewModel.unlockedIds.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader(titleRes = R.string.badges_title, onBack = onBack) }
        item {
            Text(
                text = stringResource(R.string.badges_subtitle, unlockedIds.size, BadgeId.entries.size),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
        }
        items(BadgeId.entries.toList()) { badgeId ->
            BadgeRow(badgeId = badgeId, unlocked = badgeId.name in unlockedIds)
        }
    }
}

@Composable
private fun BadgeRow(badgeId: BadgeId, unlocked: Boolean) {
    DMCard(modifier = Modifier.fillMaxWidth().alpha(if (unlocked) 1f else 0.5f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = badgeEmoji(badgeId), fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = stringResource(badgeTitleRes(badgeId)), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Text(
                    text = stringResource(if (unlocked) badgeUnlockedDescriptionRes(badgeId) else badgeLockedDescriptionRes(badgeId)),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unlocked) Accent else TextDisabled,
                )
            }
        }
    }
}

private fun badgeEmoji(badgeId: BadgeId): String = when (badgeId) {
    BadgeId.CHECKIN_STREAK_7 -> "🔥"
    BadgeId.CHECKIN_STREAK_30 -> "🏆"
    BadgeId.TBREAK_HERO_7 -> "🌿"
    BadgeId.SMOKE_FREE_3 -> "🫁"
    BadgeId.LIVER_FRIEND_7 -> "🍹"
    BadgeId.MONK_MODE_7 -> "🧘"
    BadgeId.DETOX_MASTER_3 -> "⚡"
    BadgeId.BUDGET_BOSS -> "💰"
}

private fun badgeTitleRes(badgeId: BadgeId): Int = when (badgeId) {
    BadgeId.CHECKIN_STREAK_7 -> R.string.badge_checkin_streak_7
    BadgeId.CHECKIN_STREAK_30 -> R.string.badge_checkin_streak_30
    BadgeId.TBREAK_HERO_7 -> R.string.badge_tbreak_hero_7
    BadgeId.SMOKE_FREE_3 -> R.string.badge_smoke_free_3
    BadgeId.LIVER_FRIEND_7 -> R.string.badge_liver_friend_7
    BadgeId.MONK_MODE_7 -> R.string.badge_monk_mode_7
    BadgeId.DETOX_MASTER_3 -> R.string.badge_detox_master_3
    BadgeId.BUDGET_BOSS -> R.string.badge_budget_boss
}

private fun badgeLockedDescriptionRes(badgeId: BadgeId): Int = when (badgeId) {
    BadgeId.CHECKIN_STREAK_7 -> R.string.badge_desc_checkin_streak_7
    BadgeId.CHECKIN_STREAK_30 -> R.string.badge_desc_checkin_streak_30
    BadgeId.TBREAK_HERO_7 -> R.string.badge_desc_tbreak_hero_7
    BadgeId.SMOKE_FREE_3 -> R.string.badge_desc_smoke_free_3
    BadgeId.LIVER_FRIEND_7 -> R.string.badge_desc_liver_friend_7
    BadgeId.MONK_MODE_7 -> R.string.badge_desc_monk_mode_7
    BadgeId.DETOX_MASTER_3 -> R.string.badge_desc_detox_master_3
    BadgeId.BUDGET_BOSS -> R.string.badge_desc_budget_boss
}

private fun badgeUnlockedDescriptionRes(badgeId: BadgeId): Int = R.string.badges_unlocked_caption
