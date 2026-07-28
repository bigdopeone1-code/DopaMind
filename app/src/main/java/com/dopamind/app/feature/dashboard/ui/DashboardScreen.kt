package com.dopamind.app.feature.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SmokingRooms
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.analytics.CorrelationEngine
import com.dopamind.app.core.analytics.ModuleScore
import com.dopamind.app.core.analytics.ModuleScoreCalculator
import com.dopamind.app.core.analytics.ModuleScoreStatus
import com.dopamind.app.core.analytics.model.CorrelationDirection
import com.dopamind.app.core.analytics.model.CorrelationStrength
import com.dopamind.app.core.analytics.model.DebtContributor
import com.dopamind.app.core.designsystem.DMGlassCard
import com.dopamind.app.core.designsystem.SectionHeader
import com.dopamind.app.core.designsystem.Sparkline
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.navigation.Destination
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.BackgroundPrimary
import com.dopamind.app.core.theme.Danger
import com.dopamind.app.core.theme.HeroCyan
import com.dopamind.app.core.theme.HeroPink
import com.dopamind.app.core.theme.HeroPurple
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.core.theme.Warning
import com.dopamind.app.feature.dailyvibe.data.DailyVibeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

data class InsightData(val contributorRes: Int, val positive: Boolean)

data class DashboardUiState(
    val loaded: Boolean = false,
    val healthScore: ModuleScore = ModuleScore(0, ModuleScoreStatus.FAIR),
    val sparklineValues: List<Float> = emptyList(),
    val focusScore: ModuleScore = ModuleScore(0, ModuleScoreStatus.FAIR),
    val tobaccoScore: ModuleScore = ModuleScore(0, ModuleScoreStatus.FAIR),
    val libidoScore: ModuleScore = ModuleScore(0, ModuleScoreStatus.FAIR),
    val alcoholScore: ModuleScore = ModuleScore(0, ModuleScoreStatus.FAIR),
    val recoveryScore: ModuleScore = ModuleScore(0, ModuleScoreStatus.FAIR),
    val sleepHoursAvg: Float = 0f,
    val dopamineDebtScore: Int = 0,
    val checkinStreak: Int = 0,
    val insight: InsightData? = null,
)

class DashboardViewModel(
    private val correlationEngine: CorrelationEngine,
    private val dailyVibeRepository: DailyVibeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val days30 = correlationEngine.aggregateDailyMetrics(30)
            val days7 = days30.takeLast(7)
            val debt = correlationEngine.computeDopamineDebt()
            val correlations = correlationEngine.computeSleepCorrelations()
            val streak = dailyVibeRepository.currentStreak(correlationEngine.todayEpochDay())
            val sleepAvg = days7.mapNotNull { it.sleepHours }.let { if (it.isEmpty()) 0f else it.average().toFloat() }

            val strongest = correlations.filter { it.strength != CorrelationStrength.NONE }.maxByOrNull { abs(it.coefficient) }
            val insight = strongest?.let {
                InsightData(contributorRes = contributorLabelRes(it.contributor), positive = it.direction == CorrelationDirection.POSITIVE)
            }

            _uiState.value = DashboardUiState(
                loaded = true,
                healthScore = ModuleScoreCalculator.focus(debt),
                sparklineValues = days7.map { ModuleScoreCalculator.dailyScoreProxy(it) },
                focusScore = ModuleScoreCalculator.focus(debt),
                tobaccoScore = ModuleScoreCalculator.tobacco(days7),
                libidoScore = ModuleScoreCalculator.libido(days7),
                alcoholScore = ModuleScoreCalculator.alcohol(days7),
                recoveryScore = ModuleScoreCalculator.recovery(days7),
                sleepHoursAvg = sleepAvg,
                dopamineDebtScore = debt.score,
                checkinStreak = streak,
                insight = insight,
            )
        }
    }
}

private fun contributorLabelRes(contributor: DebtContributor): Int = when (contributor) {
    DebtContributor.CANNABIS -> R.string.module_cannabis
    DebtContributor.TOBACCO -> R.string.module_tobacco
    DebtContributor.ALCOHOL -> R.string.module_alcohol
    DebtContributor.LIBIDO -> R.string.module_libido
    DebtContributor.POOR_SLEEP -> R.string.focus_contributor_poor_sleep
}

private data class ModuleCardSpec(val destination: Destination, val icon: ImageVector, val titleRes: Int)

private val moduleCards = listOf(
    ModuleCardSpec(Destination.CannabisHome, Icons.Outlined.LocalFlorist, R.string.module_cannabis),
    ModuleCardSpec(Destination.TobaccoHome, Icons.Outlined.SmokingRooms, R.string.module_tobacco),
    ModuleCardSpec(Destination.AlcoholHome, Icons.Outlined.LocalBar, R.string.module_alcohol),
    ModuleCardSpec(Destination.LibidoHome, Icons.Outlined.Favorite, R.string.module_libido),
    ModuleCardSpec(Destination.DopamineFocusHome, Icons.Outlined.Bolt, R.string.module_dopamine_focus),
    ModuleCardSpec(Destination.RecoveryHome, Icons.Outlined.NightsStay, R.string.module_recovery),
    ModuleCardSpec(Destination.FinanceHome, Icons.Outlined.AccountBalanceWallet, R.string.module_finance),
)

@Composable
fun DashboardScreen(onNavigate: (Destination) -> Unit) {
    val viewModel = dopaMindViewModel { container -> DashboardViewModel(container.correlationEngine, container.dailyVibeRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(BackgroundPrimary)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { TopZone(uiState, onSettingsClick = { onNavigate(Destination.Profile) }) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    DashboardHero(onNavigate = onNavigate, modifier = Modifier.weight(1.2f).height(320.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModuleScoreCard(Icons.Outlined.Bolt, stringResource(R.string.module_dopamine_focus), uiState.focusScore, HeroPurple) { onNavigate(Destination.DopamineFocusHome) }
                        ModuleScoreCard(Icons.Outlined.SmokingRooms, stringResource(R.string.module_tobacco), uiState.tobaccoScore, HeroCyan) { onNavigate(Destination.TobaccoHome) }
                        ModuleScoreCard(Icons.Outlined.Favorite, stringResource(R.string.module_libido), uiState.libidoScore, HeroPink) { onNavigate(Destination.LibidoHome) }
                        ModuleScoreCard(Icons.Outlined.LocalBar, stringResource(R.string.module_alcohol), uiState.alcoholScore, Warning) { onNavigate(Destination.AlcoholHome) }
                        ModuleScoreCard(Icons.Outlined.NightsStay, stringResource(R.string.module_recovery), uiState.recoveryScore, Accent) { onNavigate(Destination.RecoveryHome) }
                    }
                }
            }

            item {
                DMGlassCard(onClick = { onNavigate(Destination.DailyVibeCheckIn) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.dashboard_checkin_cta_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Accent,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.dashboard_checkin_cta_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionCard(
                        icon = Icons.Outlined.Mic,
                        label = stringResource(R.string.dashboard_quick_voice_log),
                        onClick = { onNavigate(Destination.VoiceLog) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionCard(
                        icon = Icons.Outlined.SupportAgent,
                        label = stringResource(R.string.dashboard_quick_sos),
                        onClick = { onNavigate(Destination.ChillCoachSos) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionCard(
                        icon = Icons.Outlined.EmojiEvents,
                        label = stringResource(R.string.dashboard_quick_badges),
                        onClick = { onNavigate(Destination.BadgeGallery) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item { SectionHeader(title = stringResource(R.string.dashboard_metrics_header), modifier = Modifier.fillMaxWidth()) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        label = stringResource(R.string.recovery_sleep_title),
                        value = stringResource(R.string.checkin_sleep_hours_value, uiState.sleepHoursAvg.toInt()),
                        progress = (uiState.sleepHoursAvg / 8f).coerceIn(0f, 1f),
                        color = HeroCyan,
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        label = stringResource(R.string.focus_debt_title),
                        value = "${uiState.dopamineDebtScore}",
                        progress = (uiState.dopamineDebtScore / 100f).coerceIn(0f, 1f),
                        color = Warning,
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        label = stringResource(R.string.checkin_streak_label),
                        value = stringResource(R.string.checkin_streak_days, uiState.checkinStreak),
                        progress = (uiState.checkinStreak / 7f).coerceIn(0f, 1f),
                        color = Accent,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item { InsightCard(uiState.insight) }

            item {
                SectionHeader(
                    title = stringResource(R.string.dashboard_all_modules_header),
                    trailing = stringResource(R.string.dashboard_weekly_recap_link),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(((moduleCards.size / 2 + moduleCards.size % 2) * 120).dp),
                ) {
                    items(moduleCards) { spec ->
                        ModuleCard(spec, onClick = { onNavigate(spec.destination) })
                    }
                }
            }

            item {
                DMGlassCard(onClick = { onNavigate(Destination.WeeklyRecap) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.dashboard_weekly_recap_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_weekly_recap_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopZone(uiState: DashboardUiState, onSettingsClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                Text(stringResource(R.string.dashboard_tagline), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.profile_title), tint = TextSecondary)
            }
        }
        Spacer(Modifier.height(16.dp))
        DMGlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dashboard_health_score_title), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${uiState.healthScore.value}", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                        Text("/100", style = MaterialTheme.typography.bodyLarge, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Text(statusLabel(uiState.healthScore.status), style = MaterialTheme.typography.labelMedium, color = statusColor(uiState.healthScore.status))
                }
                if (uiState.sparklineValues.size >= 2) {
                    Sparkline(values = uiState.sparklineValues, modifier = Modifier.weight(1f), lineColor = Accent)
                }
            }
        }
    }
}

@Composable
private fun ModuleScoreCard(icon: ImageVector, name: String, score: ModuleScore, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    DMGlassCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                Text("${score.value}% · ${statusLabel(score.status)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, progress: Float, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    DMGlassCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(TextSecondary.copy(alpha = 0.2f))) {
            Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(color))
        }
    }
}

@Composable
private fun InsightCard(insight: InsightData?) {
    DMGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.insight_card_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        val body = if (insight == null) {
            stringResource(R.string.insight_no_data)
        } else {
            val contributor = stringResource(insight.contributorRes)
            stringResource(if (insight.positive) R.string.insight_positive else R.string.insight_negative, contributor)
        }
        Text(body, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
    }
}

@Composable
private fun statusLabel(status: ModuleScoreStatus): String = stringResource(
    when (status) {
        ModuleScoreStatus.OPTIMAL -> R.string.status_optimal
        ModuleScoreStatus.GOOD -> R.string.status_good
        ModuleScoreStatus.FAIR -> R.string.status_fair
        ModuleScoreStatus.LOW -> R.string.status_low
    }
)

@Composable
private fun statusColor(status: ModuleScoreStatus): androidx.compose.ui.graphics.Color = when (status) {
    ModuleScoreStatus.OPTIMAL -> Accent
    ModuleScoreStatus.GOOD -> Accent
    ModuleScoreStatus.FAIR -> Warning
    ModuleScoreStatus.LOW -> Danger
}

@Composable
private fun ModuleCard(spec: ModuleCardSpec, onClick: () -> Unit) {
    DMGlassCard(onClick = onClick, modifier = Modifier.fillMaxWidth().height(108.dp)) {
        Icon(imageVector = spec.icon, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(spec.titleRes),
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
        )
    }
}

@Composable
private fun QuickActionCard(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DMGlassCard(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
        }
    }
}
