package com.dopamind.app.feature.cannabis.ui

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
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.DMChip
import com.dopamind.app.core.designsystem.DMPrimaryButton
import com.dopamind.app.core.designsystem.DMSecondaryButton
import com.dopamind.app.core.designsystem.DMTextField
import com.dopamind.app.core.designsystem.ProgressRing
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.core.designsystem.SectionHeader
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.feature.cannabis.data.CannabisLogEntity
import com.dopamind.app.feature.cannabis.data.CannabisRepository
import com.dopamind.app.feature.cannabis.data.ConsumptionMethod
import com.dopamind.app.feature.cannabis.data.TBreakEntity
import com.dopamind.app.feature.cannabis.domain.EdiblesCalculator
import com.dopamind.app.feature.cannabis.domain.EdiblesEstimate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CannabisViewModel(private val repository: CannabisRepository) : ViewModel() {
    val logs: StateFlow<List<CannabisLogEntity>> =
        repository.observeLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTBreak: StateFlow<TBreakEntity?> =
        repository.observeActiveTBreak().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logSession(strainName: String, method: ConsumptionMethod, moodBefore: Int, note: String?) {
        viewModelScope.launch {
            repository.logConsumption(
                strainName = strainName.ifBlank { "—" },
                method = method,
                thcPercent = null,
                cbdPercent = null,
                quantityGrams = null,
                moodBefore = moodBefore,
                moodAfter = null,
                note = note,
            )
        }
    }

    fun startTBreak(days: Int) = viewModelScope.launch { repository.startTBreak(days) }
    fun endTBreak() = viewModelScope.launch { activeTBreak.value?.let { repository.endTBreak(it) } }
}

@Composable
fun CannabisScreen(onBack: () -> Unit) {
    val viewModel = dopaMindViewModel { container -> CannabisViewModel(container.cannabisRepository) }
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val activeTBreak by viewModel.activeTBreak.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(titleRes = R.string.module_cannabis, onBack = onBack) }
        item { LogSessionCard(onLog = viewModel::logSession) }
        item { TBreakCard(activeTBreak, onStart = viewModel::startTBreak, onEnd = viewModel::endTBreak) }
        item { EdiblesCalculatorCard() }
        item { SectionHeader(stringResource(R.string.cannabis_recent_logs)) }
        items(logs.take(10)) { log -> LogRow(log) }
    }
}

@Composable
private fun LogSessionCard(onLog: (String, ConsumptionMethod, Int, String?) -> Unit) {
    var strainName by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(ConsumptionMethod.JOINT) }
    var moodBefore by remember { mutableStateOf(3) }
    var note by remember { mutableStateOf("") }

    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.cannabis_log_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        DMTextField(value = strainName, onValueChange = { strainName = it }, placeholder = stringResource(R.string.cannabis_strain_placeholder))
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ConsumptionMethod.entries.toList()) { m ->
                DMChip(label = methodLabel(m), selected = method == m, onClick = { method = m })
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.cannabis_mood_before), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { level ->
                DMChip(label = level.toString(), selected = moodBefore == level, onClick = { moodBefore = level })
            }
        }
        Spacer(Modifier.height(12.dp))
        DMTextField(value = note, onValueChange = { note = it }, placeholder = stringResource(R.string.cannabis_note_placeholder))
        Spacer(Modifier.height(12.dp))
        DMPrimaryButton(
            text = stringResource(R.string.cannabis_log_save),
            onClick = {
                onLog(strainName, method, moodBefore, note.ifBlank { null })
                strainName = ""
                note = ""
            },
        )
    }
}

@Composable
private fun methodLabel(method: ConsumptionMethod): String = stringResource(
    when (method) {
        ConsumptionMethod.JOINT -> R.string.cannabis_method_joint
        ConsumptionMethod.VAPE -> R.string.cannabis_method_vape
        ConsumptionMethod.EDIBLE -> R.string.cannabis_method_edible
        ConsumptionMethod.TINCTURE -> R.string.cannabis_method_tincture
        ConsumptionMethod.BONG -> R.string.cannabis_method_bong
        ConsumptionMethod.OTHER -> R.string.cannabis_method_other
    }
)

@Composable
private fun TBreakCard(active: TBreakEntity?, onStart: (Int) -> Unit, onEnd: () -> Unit) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.cannabis_tbreak_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        if (active == null) {
            var targetDays by remember { mutableStateOf(7) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 7, 14, 30).forEach { d ->
                    DMChip(label = stringResource(R.string.cannabis_tbreak_days_chip, d), selected = targetDays == d, onClick = { targetDays = d })
                }
            }
            Spacer(Modifier.height(12.dp))
            DMPrimaryButton(text = stringResource(R.string.cannabis_tbreak_start), onClick = { onStart(targetDays) })
        } else {
            val elapsedDays = ((System.currentTimeMillis() - active.startEpochMillis) / 86_400_000L).toInt()
            val progress = (elapsedDays.toFloat() / active.targetDays).coerceIn(0f, 1f)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProgressRing(progress = progress) {
                    Text("${elapsedDays}d", style = MaterialTheme.typography.labelMedium, color = Accent)
                }
                Column {
                    Text(
                        text = stringResource(R.string.cannabis_tbreak_progress, elapsedDays, active.targetDays),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    DMSecondaryButton(text = stringResource(R.string.cannabis_tbreak_end), onClick = onEnd)
                }
            }
        }
    }
}

@Composable
private fun EdiblesCalculatorCard() {
    var doseMg by remember { mutableStateOf("10") }
    var weightKg by remember { mutableStateOf("70") }
    var result by remember { mutableStateOf<EdiblesEstimate?>(null) }

    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.cannabis_edibles_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        DMTextField(value = doseMg, onValueChange = { doseMg = it }, placeholder = stringResource(R.string.cannabis_edibles_dose_placeholder), singleLine = true)
        Spacer(Modifier.height(8.dp))
        DMTextField(value = weightKg, onValueChange = { weightKg = it }, placeholder = stringResource(R.string.cannabis_edibles_weight_placeholder), singleLine = true)
        Spacer(Modifier.height(12.dp))
        DMSecondaryButton(
            text = stringResource(R.string.cannabis_edibles_calculate),
            onClick = {
                val dose = doseMg.toFloatOrNull()
                val weight = weightKg.toFloatOrNull()
                if (dose != null && weight != null && weight > 0f) {
                    result = EdiblesCalculator.estimate(dose, weight, hasToleranceExperience = false)
                }
            },
        )
        result?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.cannabis_edibles_result, intensityLabel(it.intensity), it.expectedOnsetMinutesRange.first, it.expectedOnsetMinutesRange.last),
                style = MaterialTheme.typography.bodyLarge,
                color = Accent,
            )
            Text(
                text = stringResource(R.string.cannabis_edibles_wait_hint, it.suggestedWaitMinutesBeforeRedose),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun intensityLabel(intensity: com.dopamind.app.feature.cannabis.domain.EdiblesIntensity): String = stringResource(
    when (intensity) {
        com.dopamind.app.feature.cannabis.domain.EdiblesIntensity.MICRO -> R.string.cannabis_edibles_micro
        com.dopamind.app.feature.cannabis.domain.EdiblesIntensity.LOW -> R.string.cannabis_edibles_low
        com.dopamind.app.feature.cannabis.domain.EdiblesIntensity.MODERATE -> R.string.cannabis_edibles_moderate
        com.dopamind.app.feature.cannabis.domain.EdiblesIntensity.HIGH -> R.string.cannabis_edibles_high
        com.dopamind.app.feature.cannabis.domain.EdiblesIntensity.VERY_HIGH -> R.string.cannabis_edibles_very_high
    }
)

@Composable
private fun LogRow(log: CannabisLogEntity) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = log.strainName, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        Text(text = methodLabel(ConsumptionMethod.valueOf(log.method)), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}
