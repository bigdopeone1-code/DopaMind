package com.dopamind.app.feature.tobacco.ui

import androidx.compose.foundation.layout.Arrangement
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
import com.dopamind.app.core.designsystem.SectionHeader
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.feature.tobacco.data.TobaccoLogEntity
import com.dopamind.app.feature.tobacco.data.TobaccoProductType
import com.dopamind.app.feature.tobacco.data.TobaccoRepository
import com.dopamind.app.feature.tobacco.data.TobaccoTrigger
import com.dopamind.app.feature.tobacco.domain.SmartSpacerEngine
import com.dopamind.app.feature.tobacco.domain.SpacerSuggestion
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private const val DAILY_TARGET_DEFAULT = 10

class TobaccoViewModel(private val repository: TobaccoRepository) : ViewModel() {
    val logs: StateFlow<List<TobaccoLogEntity>> =
        repository.observeLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logPuff(productType: TobaccoProductType, trigger: TobaccoTrigger?) {
        viewModelScope.launch { repository.logPuff(productType, trigger) }
    }
}

@Composable
fun TobaccoScreen(onBack: () -> Unit, onViewHistory: () -> Unit) {
    val viewModel = dopaMindViewModel { container -> TobaccoViewModel(container.tobaccoRepository) }
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    val todayStart = remember {
        LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val todayLogs = logs.filter { it.timestampEpochMillis >= todayStart }
    val suggestion = remember(todayLogs.size) {
        SmartSpacerEngine.suggest(todayLogs.map { it.timestampEpochMillis }, DAILY_TARGET_DEFAULT, System.currentTimeMillis())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(titleRes = R.string.module_tobacco, onBack = onBack) }
        item { DMSecondaryButton(text = stringResource(R.string.history_view_trend), onClick = onViewHistory) }
        item { CounterCard(todayCount = todayLogs.size, target = DAILY_TARGET_DEFAULT, onLog = viewModel::logPuff) }
        item { SmartSpacerCard(suggestion) }
        item { SectionHeader(stringResource(R.string.tobacco_recent_logs)) }
        items(logs.take(10)) { log -> LogRow(log) }
    }
}

@Composable
private fun CounterCard(todayCount: Int, target: Int, onLog: (TobaccoProductType, TobaccoTrigger?) -> Unit) {
    var productType by remember { mutableStateOf(TobaccoProductType.IQOS_STICK) }
    var trigger by remember { mutableStateOf<TobaccoTrigger?>(null) }

    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.tobacco_today_count, todayCount, target), style = MaterialTheme.typography.headlineLarge, color = Accent)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.tobacco_product_type), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TobaccoProductType.entries.toList()) { p ->
                DMChip(label = productLabel(p), selected = productType == p, onClick = { productType = p })
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.tobacco_trigger), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TobaccoTrigger.entries.toList()) { t ->
                DMChip(label = triggerLabel(t), selected = trigger == t, onClick = { trigger = if (trigger == t) null else t })
            }
        }
        Spacer(Modifier.height(12.dp))
        DMPrimaryButton(text = stringResource(R.string.tobacco_log_button), onClick = { onLog(productType, trigger) })
    }
}

@Composable
private fun SmartSpacerCard(suggestion: SpacerSuggestion) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.tobacco_spacer_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (suggestion.minutesUntilNextSuggested > 0) {
                stringResource(R.string.tobacco_spacer_wait, suggestion.minutesUntilNextSuggested)
            } else {
                stringResource(R.string.tobacco_spacer_clear)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (suggestion.onTrackForTarget) Accent else com.dopamind.app.core.theme.Warning,
        )
    }
}

@Composable
private fun productLabel(type: TobaccoProductType): String = stringResource(
    when (type) {
        TobaccoProductType.CIGARETTE -> R.string.tobacco_product_cigarette
        TobaccoProductType.IQOS_STICK -> R.string.tobacco_product_iqos
        TobaccoProductType.VAPE_PUFF -> R.string.tobacco_product_vape
        TobaccoProductType.OTHER -> R.string.tobacco_product_other
    }
)

@Composable
private fun triggerLabel(trigger: TobaccoTrigger): String = stringResource(
    when (trigger) {
        TobaccoTrigger.BOREDOM -> R.string.tobacco_trigger_boredom
        TobaccoTrigger.STRESS -> R.string.tobacco_trigger_stress
        TobaccoTrigger.SOCIAL -> R.string.tobacco_trigger_social
        TobaccoTrigger.HABIT -> R.string.tobacco_trigger_habit
        TobaccoTrigger.AFTER_MEAL -> R.string.tobacco_trigger_after_meal
        TobaccoTrigger.ALCOHOL -> R.string.tobacco_trigger_alcohol
        TobaccoTrigger.OTHER -> R.string.tobacco_trigger_other
    }
)

@Composable
private fun LogRow(log: TobaccoLogEntity) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = productLabel(TobaccoProductType.valueOf(log.productType)), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            log.trigger?.let { Text(text = triggerLabel(TobaccoTrigger.valueOf(it)), style = MaterialTheme.typography.labelMedium, color = TextSecondary) }
        }
    }
}
