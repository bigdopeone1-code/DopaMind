package com.dopamind.app.feature.libido.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
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
import com.dopamind.app.core.ai.coach.BreathingPhase
import com.dopamind.app.core.ai.coach.BreathingStep
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.DMChip
import com.dopamind.app.core.designsystem.DMPrimaryButton
import com.dopamind.app.core.designsystem.ProgressRing
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.feature.libido.data.LibidoActivityType
import com.dopamind.app.feature.libido.data.LibidoLogEntity
import com.dopamind.app.feature.libido.data.LibidoRepository
import com.dopamind.app.feature.libido.data.StreakCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val RESET_BREATHING_PATTERN = listOf(
    BreathingStep(BreathingPhase.INHALE, 4),
    BreathingStep(BreathingPhase.HOLD, 4),
    BreathingStep(BreathingPhase.EXHALE, 4),
    BreathingStep(BreathingPhase.REST, 4),
)
private const val STREAK_MILESTONE_DAYS = 7

class LibidoViewModel(private val repository: LibidoRepository) : ViewModel() {
    val logs: StateFlow<List<LibidoLogEntity>> =
        repository.observeLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logActivity(type: LibidoActivityType, moodBefore: Int) {
        viewModelScope.launch { repository.logActivity(type, moodBefore, moodAfter = null, note = null) }
    }
}

@Composable
fun LibidoScreen(onBack: () -> Unit) {
    val viewModel = dopaMindViewModel { container -> LibidoViewModel(container.libidoRepository) }
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val lastLogMillis = logs.maxByOrNull { it.timestampEpochMillis }?.timestampEpochMillis
    val streakDays = StreakCalculator.daysSince(lastLogMillis, System.currentTimeMillis())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(titleRes = R.string.module_libido, onBack = onBack) }
        item { LogCard(onLog = viewModel::logActivity) }
        item { StreakCard(streakDays) }
        item { DopamineResetCard() }
    }
}

@Composable
private fun LogCard(onLog: (LibidoActivityType, Int) -> Unit) {
    var type by remember { mutableStateOf(LibidoActivityType.SOLO) }
    var mood by remember { mutableStateOf(3) }

    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.libido_log_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DMChip(label = stringResource(R.string.libido_type_solo), selected = type == LibidoActivityType.SOLO, onClick = { type = LibidoActivityType.SOLO })
            DMChip(label = stringResource(R.string.libido_type_partner), selected = type == LibidoActivityType.PARTNER, onClick = { type = LibidoActivityType.PARTNER })
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.libido_mood_label), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { level -> DMChip(label = level.toString(), selected = mood == level, onClick = { mood = level }) }
        }
        Spacer(Modifier.height(12.dp))
        DMPrimaryButton(text = stringResource(R.string.libido_log_save), onClick = { onLog(type, mood) })
    }
}

@Composable
private fun StreakCard(streakDays: Int) {
    val progress = (streakDays.toFloat() / STREAK_MILESTONE_DAYS).coerceIn(0f, 1f)
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ProgressRing(progress = progress) {
                Text("${streakDays}d", style = MaterialTheme.typography.labelMedium, color = Accent)
            }
            Text(stringResource(R.string.libido_streak_caption, streakDays), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        }
    }
}

@Composable
private fun DopamineResetCard() {
    var running by remember { mutableStateOf(false) }
    var stepIndex by remember { mutableStateOf(0) }
    var secondsLeft by remember { mutableStateOf(RESET_BREATHING_PATTERN.first().durationSeconds) }

    LaunchedEffect(running, stepIndex) {
        if (!running) return@LaunchedEffect
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
        val next = (stepIndex + 1) % RESET_BREATHING_PATTERN.size
        stepIndex = next
        secondsLeft = RESET_BREATHING_PATTERN[next].durationSeconds
    }

    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.libido_reset_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        if (running) {
            Text(
                text = "${phaseLabel(RESET_BREATHING_PATTERN[stepIndex].phase)} · ${secondsLeft}s",
                style = MaterialTheme.typography.headlineLarge,
                color = Accent,
            )
            Spacer(Modifier.height(12.dp))
            DMPrimaryButton(text = stringResource(R.string.libido_reset_stop), onClick = { running = false; stepIndex = 0; secondsLeft = RESET_BREATHING_PATTERN.first().durationSeconds })
        } else {
            DMPrimaryButton(text = stringResource(R.string.libido_reset_start), onClick = { running = true })
        }
    }
}

@Composable
private fun phaseLabel(phase: BreathingPhase): String = stringResource(
    when (phase) {
        BreathingPhase.INHALE -> R.string.breathing_inhale
        BreathingPhase.HOLD -> R.string.breathing_hold
        BreathingPhase.EXHALE -> R.string.breathing_exhale
        BreathingPhase.REST -> R.string.breathing_rest
    }
)
