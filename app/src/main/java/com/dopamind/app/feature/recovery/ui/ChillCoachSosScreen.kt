package com.dopamind.app.feature.recovery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.ai.coach.BreathingPhase
import com.dopamind.app.core.ai.coach.ChillCoachEngine
import com.dopamind.app.core.ai.coach.ChillCoachMessage
import com.dopamind.app.core.ai.coach.ChillCoachSession
import com.dopamind.app.core.designsystem.DMPrimaryButton
import com.dopamind.app.core.designsystem.ProgressRing
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChillCoachSosViewModel(private val engine: ChillCoachEngine) : ViewModel() {
    private val _session = MutableStateFlow<ChillCoachSession?>(null)
    val session: StateFlow<ChillCoachSession?> = _session.asStateFlow()

    init {
        viewModelScope.launch {
            _session.value = engine.buildSession()
            engine.recordSessionUsed()
        }
    }
}

@Composable
fun ChillCoachSosScreen(onBack: () -> Unit) {
    val viewModel = dopaMindViewModel { container -> ChillCoachSosViewModel(container.chillCoachEngine) }
    val session by viewModel.session.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        session?.let { SosContent(it) }
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close), tint = TextSecondary)
        }
    }
}

@Composable
private fun SosContent(session: ChillCoachSession) {
    var messageIndex by remember { mutableStateOf(-1) } // -1 = opening message

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BreathingRing(session.breathingPattern.map { it.phase to it.durationSeconds })

        Text(
            text = stringResource(messageRes(if (messageIndex == -1) session.openingMessage else session.reassuranceSequence[messageIndex])),
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 32.dp, bottom = 32.dp),
        )

        DMPrimaryButton(
            text = stringResource(if (messageIndex < session.reassuranceSequence.lastIndex) R.string.sos_next else R.string.sos_done),
            onClick = { if (messageIndex < session.reassuranceSequence.lastIndex) messageIndex++ },
        )
    }
}

@Composable
private fun BreathingRing(pattern: List<Pair<BreathingPhase, Int>>) {
    var stepIndex by remember { mutableStateOf(0) }
    var secondsLeft by remember { mutableStateOf(pattern.first().second) }

    LaunchedEffect(stepIndex) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
        stepIndex = (stepIndex + 1) % pattern.size
        secondsLeft = pattern[stepIndex].second
    }

    val (phase, totalSeconds) = pattern[stepIndex]
    val progress = 1f - (secondsLeft.toFloat() / totalSeconds)

    ProgressRing(progress = progress, size = 140.dp, strokeWidth = 6.dp) {
        Text(text = stringResource(phaseRes(phase)), style = MaterialTheme.typography.titleLarge, color = Accent)
    }
}

private fun phaseRes(phase: BreathingPhase): Int = when (phase) {
    BreathingPhase.INHALE -> R.string.breathing_inhale
    BreathingPhase.HOLD -> R.string.breathing_hold
    BreathingPhase.EXHALE -> R.string.breathing_exhale
    BreathingPhase.REST -> R.string.breathing_rest
}

private fun messageRes(message: ChillCoachMessage): Int = when (message) {
    ChillCoachMessage.WELCOME_FIRST_TIME -> R.string.sos_welcome_first_time
    ChillCoachMessage.WELCOME_RETURNING -> R.string.sos_welcome_returning
    ChillCoachMessage.REASSURANCE_TEMPORARY -> R.string.sos_reassurance_temporary
    ChillCoachMessage.REASSURANCE_SAFE_PLACE -> R.string.sos_reassurance_safe_place
    ChillCoachMessage.REASSURANCE_BREATHE -> R.string.sos_reassurance_breathe
    ChillCoachMessage.REASSURANCE_PASSES -> R.string.sos_reassurance_passes
    ChillCoachMessage.GROUNDING_5_4_3_2_1 -> R.string.sos_grounding
    ChillCoachMessage.REASSURANCE_LOW_MOOD_CONTEXT -> R.string.sos_reassurance_low_mood
}
