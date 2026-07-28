package com.dopamind.app.feature.alcohol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.ai.hangover.PredictiveHangoverEngine
import com.dopamind.app.core.analytics.model.HangoverRiskLevel
import com.dopamind.app.core.analytics.model.HangoverTip
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.DMChip
import com.dopamind.app.core.designsystem.DMGlassCard
import com.dopamind.app.core.designsystem.DMPrimaryButton
import com.dopamind.app.core.designsystem.DMSecondaryButton
import com.dopamind.app.core.designsystem.ProgressRing
import com.dopamind.app.core.designsystem.SectionHeader
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.Danger
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.core.theme.Warning
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.feature.alcohol.data.AlcoholRepository
import com.dopamind.app.feature.alcohol.data.DrinkLogEntity
import com.dopamind.app.feature.alcohol.data.DrinkType
import com.dopamind.app.feature.alcohol.domain.BacCalculator
import com.dopamind.app.feature.alcohol.domain.BiologicalSex
import com.dopamind.app.feature.alcohol.domain.DrinkInput
import com.dopamind.app.feature.profile.data.ProfileRepository
import com.dopamind.app.feature.profile.data.UserProfileEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SESSION_WINDOW_HOURS = 12L

class AlcoholViewModel(
    private val repository: AlcoholRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {
    val logs: StateFlow<List<DrinkLogEntity>> =
        repository.observeLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile: StateFlow<UserProfileEntity?> =
        profileRepository.observeProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logDrink(type: DrinkType, priceEuros: Float?) {
        viewModelScope.launch {
            repository.logDrink(type, type.defaultVolumeMl, type.defaultAbvPercent, priceEuros, venue = null)
        }
    }
}

@Composable
fun AlcoholScreen(onBack: () -> Unit, onScanLabel: () -> Unit, onViewHistory: () -> Unit) {
    val viewModel = dopaMindViewModel { container -> AlcoholViewModel(container.alcoholRepository, container.profileRepository) }
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    val now = System.currentTimeMillis()
    val sessionStart = now - SESSION_WINDOW_HOURS * 3_600_000L
    val tonightLogs = logs.filter { it.timestampEpochMillis >= sessionStart }

    var weightKg by remember { mutableStateOf(70f) }
    var sex by remember { mutableStateOf(BiologicalSex.OTHER) }
    var waterGlasses by remember { mutableStateOf(0) }
    var ateFood by remember { mutableStateOf(true) }
    var plannedSleepHours by remember { mutableStateOf(7f) }

    LaunchedEffect(profile) {
        profile?.let {
            weightKg = it.weightKg
            sex = runCatching { BiologicalSex.valueOf(it.biologicalSex) }.getOrDefault(BiologicalSex.OTHER)
        }
    }

    val averageDrinksLastMonth = remember(logs.size) {
        val monthAgo = now - 30L * 86_400_000L
        logs.count { it.timestampEpochMillis >= monthAgo } / 30.0
    }

    val liveAssessment = remember(tonightLogs.size, weightKg, sex, waterGlasses, ateFood, plannedSleepHours) {
        val drinks = tonightLogs.map { DrinkInput(BacCalculator.gramsOfAlcohol(it.volumeMl, it.abvPercent), it.timestampEpochMillis) }
        PredictiveHangoverEngine.assessLive(
            drinksTonight = drinks,
            bodyWeightKg = weightKg.toDouble(),
            sex = sex,
            glassesOfWaterSoFar = waterGlasses,
            ateFoodTonight = ateFood,
            plannedSleepHours = plannedSleepHours,
            averageDrinksLastMonth = averageDrinksLastMonth,
            nowEpochMillis = now,
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(titleRes = R.string.module_alcohol, onBack = onBack) }
        item { DMSecondaryButton(text = stringResource(R.string.history_view_trend), onClick = onViewHistory) }
        item { DrinkCounterCard(tonightCount = tonightLogs.size, onLog = viewModel::logDrink, onScanLabel = onScanLabel) }
        item {
            BodyProfileCard(
                weightKg = weightKg,
                onWeightChange = { weightKg = it },
                sex = sex,
                onSexChange = { sex = it },
            )
        }
        item { BacCard(liveAssessment.bac.bacGramsPerLiter, liveAssessment.bac.isStillRising) }
        item { CanIDriveCard(liveAssessment.bac.bacGramsPerLiter, liveAssessment.bac.isStillRising) }
        item {
            HangoverRiskCard(
                riskScore = liveAssessment.risk.riskScore,
                level = liveAssessment.risk.level,
                tips = liveAssessment.risk.tips,
                waterGlasses = waterGlasses,
                onWaterGlassesChange = { waterGlasses = it },
                ateFood = ateFood,
                onAteFoodChange = { ateFood = it },
            )
        }
        item { SectionHeader(stringResource(R.string.alcohol_recent_logs)) }
        items(logs.take(10)) { log -> LogRow(log) }
    }
}

private val DirectDrinkTypes = listOf(DrinkType.BEER, DrinkType.WINE, DrinkType.SPIRIT_SHOT, DrinkType.OTHER)
private val CocktailDrinkTypes = listOf(DrinkType.SPRITZ, DrinkType.NEGRONI, DrinkType.MOJITO, DrinkType.MARGARITA, DrinkType.GIN_TONIC, DrinkType.COCKTAIL_OTHER)

@Composable
private fun DrinkCounterCard(tonightCount: Int, onLog: (DrinkType, Float?) -> Unit, onScanLabel: () -> Unit) {
    var showCocktails by remember { mutableStateOf(false) }
    DMGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.alcohol_tonight_count, tonightCount), style = MaterialTheme.typography.headlineLarge, color = Accent)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DirectDrinkTypes) { type ->
                DMChip(label = drinkTypeLabel(type), selected = false, onClick = { onLog(type, null) })
            }
            item {
                DMChip(label = stringResource(R.string.drink_type_cocktail), selected = showCocktails, onClick = { showCocktails = !showCocktails })
            }
        }
        if (showCocktails) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CocktailDrinkTypes) { type ->
                    DMChip(label = drinkTypeLabel(type), selected = false, onClick = { onLog(type, null); showCocktails = false })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        DMSecondaryButton(text = stringResource(R.string.alcohol_scan_label), onClick = onScanLabel)
    }
}

@Composable
private fun BodyProfileCard(weightKg: Float, onWeightChange: (Float) -> Unit, sex: BiologicalSex, onSexChange: (BiologicalSex) -> Unit) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.alcohol_profile_title), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(55f, 65f, 75f, 85f, 95f).forEach { w ->
                DMChip(label = "${w.toInt()}kg", selected = weightKg == w, onClick = { onWeightChange(w) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BiologicalSex.entries.forEach { s ->
                DMChip(label = sexLabel(s), selected = sex == s, onClick = { onSexChange(s) })
            }
        }
    }
}

@Composable
private fun sexLabel(sex: BiologicalSex): String = stringResource(
    when (sex) {
        BiologicalSex.MALE -> R.string.alcohol_sex_male
        BiologicalSex.FEMALE -> R.string.alcohol_sex_female
        BiologicalSex.OTHER -> R.string.alcohol_sex_other
    }
)

@Composable
private fun BacCard(bacGramsPerLiter: Double, isRising: Boolean) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ProgressRing(progress = (bacGramsPerLiter / 1.5).toFloat(), progressColor = if (bacGramsPerLiter >= 0.5) Danger else Accent) {
                Text(String.format("%.2f", bacGramsPerLiter), style = MaterialTheme.typography.labelMedium, color = TextPrimary)
            }
            Column {
                Text(stringResource(R.string.alcohol_bac_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(
                    text = stringResource(if (isRising) R.string.alcohol_bac_rising else R.string.alcohol_bac_stable),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun CanIDriveCard(bacGramsPerLiter: Double, isRising: Boolean) {
    val assessment = remember(bacGramsPerLiter, isRising) {
        BacCalculator.assessDriving(
            com.dopamind.app.feature.alcohol.domain.BacResult(bacGramsPerLiter, 0.0, 0.0, isRising),
        )
    }
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.alcohol_can_drive_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(if (assessment.canLikelyDrive) R.string.alcohol_can_drive_yes else R.string.alcohol_can_drive_no),
            style = MaterialTheme.typography.bodyLarge,
            color = if (assessment.canLikelyDrive) Accent else Danger,
        )
        if (!assessment.canLikelyDrive && assessment.hoursUntilLegalEstimate > 0) {
            Text(
                text = stringResource(R.string.alcohol_can_drive_hours, assessment.hoursUntilLegalEstimate),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
        Text(
            text = stringResource(R.string.alcohol_can_drive_disclaimer),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun HangoverRiskCard(
    riskScore: Int,
    level: HangoverRiskLevel,
    tips: List<HangoverTip>,
    waterGlasses: Int,
    onWaterGlassesChange: (Int) -> Unit,
    ateFood: Boolean,
    onAteFoodChange: (Boolean) -> Unit,
) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.alcohol_hangover_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.alcohol_hangover_score, riskScore, levelLabel(level)),
            style = MaterialTheme.typography.bodyLarge,
            color = riskColor(level),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..5).forEach { g ->
                DMChip(label = g.toString(), selected = waterGlasses == g, onClick = { onWaterGlassesChange(g) })
            }
        }
        Spacer(Modifier.height(8.dp))
        DMChip(label = stringResource(R.string.alcohol_ate_food), selected = ateFood, onClick = { onAteFoodChange(!ateFood) })
        if (tips.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            tips.forEach { tip -> Text(text = "• " + stringResource(tipLabel(tip)), style = MaterialTheme.typography.labelMedium, color = TextSecondary) }
        }
    }
}

@Composable
private fun riskColor(level: HangoverRiskLevel) = when (level) {
    HangoverRiskLevel.LOW -> Accent
    HangoverRiskLevel.MODERATE -> Warning
    HangoverRiskLevel.HIGH, HangoverRiskLevel.SEVERE -> Danger
}

@Composable
private fun levelLabel(level: HangoverRiskLevel): String = stringResource(
    when (level) {
        HangoverRiskLevel.LOW -> R.string.hangover_level_low
        HangoverRiskLevel.MODERATE -> R.string.hangover_level_moderate
        HangoverRiskLevel.HIGH -> R.string.hangover_level_high
        HangoverRiskLevel.SEVERE -> R.string.hangover_level_severe
    }
)

@Composable
private fun tipLabel(tip: HangoverTip): Int = when (tip) {
    HangoverTip.DRINK_WATER -> R.string.hangover_tip_water
    HangoverTip.EAT_BEFORE_SLEEP -> R.string.hangover_tip_eat
    HangoverTip.ELECTROLYTES -> R.string.hangover_tip_electrolytes
    HangoverTip.SLEEP_EARLY -> R.string.hangover_tip_sleep_early
    HangoverTip.SLOW_DOWN -> R.string.hangover_tip_slow_down
    HangoverTip.STOP_DRINKING -> R.string.hangover_tip_stop
}

@Composable
internal fun drinkTypeLabel(type: DrinkType): String = stringResource(
    when (type) {
        DrinkType.BEER -> R.string.drink_type_beer
        DrinkType.WINE -> R.string.drink_type_wine
        DrinkType.SPIRIT_SHOT -> R.string.drink_type_spirit
        DrinkType.SPRITZ -> R.string.drink_type_spritz
        DrinkType.NEGRONI -> R.string.drink_type_negroni
        DrinkType.MOJITO -> R.string.drink_type_mojito
        DrinkType.MARGARITA -> R.string.drink_type_margarita
        DrinkType.GIN_TONIC -> R.string.drink_type_gin_tonic
        DrinkType.COCKTAIL_OTHER -> R.string.drink_type_cocktail_other
        DrinkType.OTHER -> R.string.drink_type_other
    }
)

@Composable
private fun LogRow(log: DrinkLogEntity) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = drinkTypeLabel(DrinkType.valueOf(log.drinkType)), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
    }
}
