package com.dopamind.app.feature.home.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.DMChip
import com.dopamind.app.core.designsystem.ProgressRing
import com.dopamind.app.core.designsystem.SectionHeader
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.habit.BodySystem
import com.dopamind.app.core.habit.MeasurementKind
import com.dopamind.app.core.navigation.Destination
import com.dopamind.app.core.scoring.BodySystemStatus
import com.dopamind.app.core.scoring.DimensionScore
import com.dopamind.app.core.scoring.DopaScore
import com.dopamind.app.core.scoring.ScoreBand
import com.dopamind.app.core.scoring.ScoreDimension
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.HeroBlue
import com.dopamind.app.core.theme.HeroCyan
import com.dopamind.app.core.theme.TextDisabled
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.core.theme.Warning

/** Score bands never read as praise or blame — see ScoreBand's contract. */
@Composable
private fun bandLabel(band: ScoreBand): String = stringResource(
    when (band) {
        ScoreBand.STRONG -> R.string.score_band_strong
        ScoreBand.GOOD -> R.string.score_band_good
        ScoreBand.STEADY -> R.string.score_band_steady
        ScoreBand.UNDER_LOAD -> R.string.score_band_under_load
    }
)

private fun bandColor(band: ScoreBand): Color = when (band) {
    ScoreBand.STRONG, ScoreBand.GOOD -> Accent
    ScoreBand.STEADY -> HeroCyan
    ScoreBand.UNDER_LOAD -> Warning
}

@Composable
private fun dimensionLabel(dimension: ScoreDimension): String = stringResource(
    when (dimension) {
        ScoreDimension.RECOVERY -> R.string.dimension_recovery
        ScoreDimension.SLEEP -> R.string.dimension_sleep
        ScoreDimension.FOCUS -> R.string.dimension_focus
        ScoreDimension.ENERGY -> R.string.dimension_energy
        ScoreDimension.STRESS -> R.string.dimension_stress
        ScoreDimension.HABIT_LOAD -> R.string.dimension_habit_load
    }
)

@Composable
private fun systemLabel(system: BodySystem): String = stringResource(
    when (system) {
        BodySystem.BRAIN -> R.string.system_brain
        BodySystem.HEART -> R.string.system_heart
        BodySystem.LUNGS -> R.string.system_lungs
        BodySystem.LIVER -> R.string.system_liver
        BodySystem.RECOVERY -> R.string.system_recovery
    }
)

@Composable
fun HomeScreen(onNavigate: (Destination) -> Unit) {
    val viewModel = dopaMindViewModel { container ->
        HomeViewModel(
            behaviorEventRepository = container.behaviorEventRepository,
            profileRepository = container.profileRepository,
            scoreInputBuilder = container.scoreInputBuilder,
            dopaScoreEngine = container.dopaScoreEngine,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { GreetingHeader(state) }
        item { DopaScoreCard(state.score) }
        item { BodyStatusCard(state.bodySystems, onNavigate = onNavigate) }
        item { SectionHeader(stringResource(R.string.home_today_title)) }
        items(state.todayTotals, key = { it.category.name }) { total ->
            TodayCategoryRow(
                total = total,
                onQuickLog = { amount -> viewModel.quickLog(total.category, amount.toFloat()) },
                onOpen = { onNavigate(Destination.AddEvent(total.category.name)) },
            )
        }
        item { InsightCard(state.score) }
    }
}

@Composable
private fun GreetingHeader(state: HomeUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val greeting = stringResource(state.greeting.labelRes)
        Text(
            text = if (state.displayName.isBlank()) greeting else "$greeting, ${state.displayName}",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
    }
}

@Composable
private fun DopaScoreCard(score: DopaScore?) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.dopascore_title), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(12.dp))

        if (score == null || score.coverage <= 0f) {
            // Nothing logged yet: say so plainly instead of rendering a zero the
            // user would read as a bad result.
            Text(
                text = stringResource(R.string.dopascore_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
            return@DMCard
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ProgressRing(
                progress = score.value / 100f,
                size = 96.dp,
                strokeWidth = 8.dp,
                progressColor = bandColor(score.band),
            ) {
                Text(text = "${score.value}", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            }
            Column {
                Text(
                    text = bandLabel(score.band),
                    style = MaterialTheme.typography.titleLarge,
                    color = bandColor(score.band),
                )
                Text(
                    text = stringResource(R.string.dopascore_out_of),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                if (score.coverage < 1f) {
                    Spacer(Modifier.height(6.dp))
                    // Honesty rail: the score only speaks for the dimensions that
                    // actually had data behind them, and says so.
                    Text(
                        text = stringResource(R.string.dopascore_coverage, (score.coverage * 100).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextDisabled,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        score.availableDimensions.forEach { DimensionRow(it) }
    }
}

@Composable
private fun DimensionRow(dimension: DimensionScore) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = dimensionLabel(dimension.dimension),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            modifier = Modifier.width(110.dp),
        )
        Box(modifier = Modifier.weight(1f)) { LinearMeter(fraction = dimension.value / 100f) }
        Text(text = "${dimension.value}", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
    }
    Spacer(Modifier.height(8.dp))
}

/** A thin capsule meter — the shared visual language for every 0-100 reading. */
@Composable
private fun LinearMeter(fraction: Float, color: Color = Accent) {
    Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
        val radius = size.height / 2f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f),
            cornerRadius = CornerRadius(radius, radius),
        )
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped > 0f) {
            drawRoundRect(
                color = color,
                size = Size(size.width * clamped, size.height),
                cornerRadius = CornerRadius(radius, radius),
            )
        }
    }
}

/**
 * The body reading panel.
 *
 * The interactive 3D model deliberately does **not** render inside this list.
 * SceneView's camera manipulator consumes drag gestures, so a Scene nested in a
 * vertically scrolling LazyColumn swallows the scroll and strands the user on
 * the Home screen. Rather than ship a Home you cannot scroll past, the readings
 * live here and the model itself opens full-screen — where pinch, pan and
 * rotate are unambiguous and actually useful — one tap away.
 */
@Composable
private fun BodyStatusCard(systems: List<BodySystemStatus>, onNavigate: (Destination) -> Unit) {
    DMCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(Destination.Dashboard) }) {
        Text(stringResource(R.string.home_body_status_title), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        // Required framing, not decoration: these readings describe logged
        // behaviour, never the state of an organ.
        Text(
            text = stringResource(R.string.home_body_status_disclaimer),
            style = MaterialTheme.typography.labelMedium,
            color = TextDisabled,
        )
        Spacer(Modifier.height(12.dp))

        systems.forEach { status ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = systemLabel(status.system),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier.width(78.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    LinearMeter(fraction = status.value / 100f, color = HeroBlue)
                }
                Text(text = "${status.value}", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            }
            Spacer(Modifier.height(10.dp))
        }

        Text(
            text = stringResource(R.string.home_body_open_model),
            style = MaterialTheme.typography.labelMedium,
            color = Accent,
        )
    }
}

@Composable
private fun TodayCategoryRow(total: TodayCategoryTotal, onQuickLog: (Int) -> Unit, onOpen: () -> Unit) {
    DMCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(total.category.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                )
                Text(
                    text = formattedAmount(total),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (total.eventCount == 0) TextDisabled else TextSecondary,
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(total.category.quickAmounts) { amount ->
                    DMChip(label = "+$amount", selected = false, onClick = { onQuickLog(amount) })
                }
            }
        }
    }
}

@Composable
private fun formattedAmount(total: TodayCategoryTotal): String {
    if (total.eventCount == 0) return stringResource(R.string.home_nothing_logged)
    val unit = stringResource(total.category.unitRes)
    val amount = when (total.category.measurement) {
        MeasurementKind.COUNT, MeasurementKind.DURATION_MINUTES -> total.amount.toInt().toString()
    }
    return "$amount $unit"
}

/**
 * One plain-language observation, drawn from whichever dimension is currently
 * furthest from the rest. Descriptive by design — it names a pattern and never
 * tells the user they did badly.
 */
@Composable
private fun InsightCard(score: DopaScore?) {
    val weakest = score?.weakestDimension ?: return
    val strongest = score.strongestDimension

    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.home_insight_title), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_insight_lowest, dimensionLabel(weakest.dimension), weakest.value),
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
        )
        if (strongest != null && strongest.dimension != weakest.dimension) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_insight_highest, dimensionLabel(strongest.dimension), strongest.value),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
        }
    }
}
