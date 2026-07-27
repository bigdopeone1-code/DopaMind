package com.dopamind.app.feature.dopaminefocus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.dopamind.app.core.analytics.CorrelationEngine
import com.dopamind.app.core.analytics.model.DebtContributor
import com.dopamind.app.core.analytics.model.DopamineDebtResult
import com.dopamind.app.core.analytics.model.Trend
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.DMChip
import com.dopamind.app.core.designsystem.DMPrimaryButton
import com.dopamind.app.core.designsystem.DMTextField
import com.dopamind.app.core.designsystem.ProgressRing
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.Danger
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.core.theme.Warning
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.feature.dopaminefocus.data.DetoxSessionEntity
import com.dopamind.app.feature.dopaminefocus.data.DopamineFocusRepository
import com.dopamind.app.feature.dopaminefocus.data.StackingType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DopamineFocusViewModel(
    private val repository: DopamineFocusRepository,
    private val correlationEngine: CorrelationEngine,
) : ViewModel() {

    private val _debt = MutableStateFlow<DopamineDebtResult?>(null)
    val debt: StateFlow<DopamineDebtResult?> = _debt.asStateFlow()

    val activeDetox: StateFlow<DetoxSessionEntity?> =
        repository.observeActiveDetoxSession().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch { _debt.value = correlationEngine.computeDopamineDebt() }
    }

    fun logWhy(triggerModule: String, reason: String) {
        viewModelScope.launch { repository.logWhy(triggerModule, reason) }
    }

    fun startStacking(dutyLabel: String, pleasureLabel: String) {
        viewModelScope.launch {
            repository.startStackingSession(dutyLabel, StackingType.DUTY)
            repository.startStackingSession(pleasureLabel, StackingType.PLEASURE)
        }
    }

    fun startDetox(targetHours: Int) = viewModelScope.launch { repository.startDetox(targetHours) }

    fun endDetox(completed: Boolean) = viewModelScope.launch {
        activeDetox.value?.let { repository.endDetox(it, completed) }
    }
}

@Composable
fun DopamineFocusScreen(onBack: () -> Unit) {
    val viewModel = dopaMindViewModel { container ->
        DopamineFocusViewModel(container.dopamineFocusRepository, container.correlationEngine)
    }
    val debt by viewModel.debt.collectAsStateWithLifecycle()
    val activeDetox by viewModel.activeDetox.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(titleRes = R.string.module_dopamine_focus, onBack = onBack) }
        item { DopamineDebtCard(debt) }
        item { HealthStackingCard(onStart = viewModel::startStacking) }
        item { DetoxModeCard(activeDetox, onStart = viewModel::startDetox, onEnd = viewModel::endDetox) }
        item { WhyPromptCard(onLog = viewModel::logWhy) }
    }
}

@Composable
private fun DopamineDebtCard(debt: DopamineDebtResult?) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.focus_debt_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        if (debt == null) {
            Text(stringResource(R.string.recap_loading), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        } else {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProgressRing(progress = debt.score / 100f, progressColor = debtColor(debt.score)) {
                    Text("${debt.score}", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                }
                Column {
                    Text(stringResource(trendLabel(debt.trend)), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    if (debt.topContributors.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.focus_debt_top_contributors, debt.topContributors.joinToString(", ") { stringResource(contributorLabel(it)) }),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun debtColor(score: Int) = when {
    score < 35 -> Accent
    score < 65 -> Warning
    else -> Danger
}

@Composable
private fun trendLabel(trend: Trend): Int = when (trend) {
    Trend.RISING -> R.string.focus_debt_trend_rising
    Trend.FALLING -> R.string.focus_debt_trend_falling
    Trend.STABLE -> R.string.focus_debt_trend_stable
}

@Composable
private fun contributorLabel(contributor: DebtContributor): Int = when (contributor) {
    DebtContributor.CANNABIS -> R.string.module_cannabis
    DebtContributor.TOBACCO -> R.string.module_tobacco
    DebtContributor.ALCOHOL -> R.string.module_alcohol
    DebtContributor.LIBIDO -> R.string.module_libido
    DebtContributor.POOR_SLEEP -> R.string.focus_contributor_poor_sleep
}

@Composable
private fun HealthStackingCard(onStart: (String, String) -> Unit) {
    var duty by remember { mutableStateOf("") }
    var pleasure by remember { mutableStateOf("") }
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.focus_stacking_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.focus_stacking_subtitle), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        DMTextField(value = duty, onValueChange = { duty = it }, placeholder = stringResource(R.string.focus_stacking_duty_placeholder), singleLine = true)
        Spacer(Modifier.height(8.dp))
        DMTextField(value = pleasure, onValueChange = { pleasure = it }, placeholder = stringResource(R.string.focus_stacking_pleasure_placeholder), singleLine = true)
        Spacer(Modifier.height(12.dp))
        DMPrimaryButton(
            text = stringResource(R.string.focus_stacking_start),
            onClick = { if (duty.isNotBlank() && pleasure.isNotBlank()) { onStart(duty, pleasure); duty = ""; pleasure = "" } },
        )
    }
}

@Composable
private fun DetoxModeCard(active: DetoxSessionEntity?, onStart: (Int) -> Unit, onEnd: (Boolean) -> Unit) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.focus_detox_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        if (active == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 6, 12, 24).forEach { h ->
                    DMChip(label = stringResource(R.string.focus_detox_hours_chip, h), selected = false, onClick = { onStart(h) })
                }
            }
        } else {
            val elapsedHours = (System.currentTimeMillis() - active.startEpochMillis) / 3_600_000f
            val progress = (elapsedHours / active.targetHours).coerceIn(0f, 1f)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProgressRing(progress = progress) {
                    Text(stringResource(R.string.focus_detox_hours_chip, elapsedHours.toInt()), style = MaterialTheme.typography.labelMedium, color = Accent)
                }
                Column {
                    Text(stringResource(R.string.focus_detox_target, active.targetHours), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    DMPrimaryButton(text = stringResource(R.string.focus_detox_complete), onClick = { onEnd(true) })
                }
            }
        }
    }
}

@Composable
private fun WhyPromptCard(onLog: (String, String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.focus_why_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.focus_why_subtitle), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        DMTextField(value = reason, onValueChange = { reason = it }, placeholder = stringResource(R.string.focus_why_placeholder), minLines = 2)
        Spacer(Modifier.height(12.dp))
        DMPrimaryButton(
            text = stringResource(R.string.focus_why_save),
            onClick = { if (reason.isNotBlank()) { onLog("general", reason); reason = "" } },
        )
    }
}
