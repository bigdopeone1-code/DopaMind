package com.dopamind.app.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.analytics.CorrelationEngine
import com.dopamind.app.core.analytics.model.DailyMetrics
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.core.designsystem.SimpleBarChart
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.navigation.HistoryModule
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val HISTORY_DAYS = 30

class ModuleHistoryViewModel(
    private val module: HistoryModule,
    private val correlationEngine: CorrelationEngine,
) : ViewModel() {
    private val _days = MutableStateFlow<List<DailyMetrics>?>(null)
    val days: StateFlow<List<DailyMetrics>?> = _days.asStateFlow()

    init {
        viewModelScope.launch { _days.value = correlationEngine.aggregateDailyMetrics(HISTORY_DAYS) }
    }

    fun valuesFor(days: List<DailyMetrics>): List<Float> = days.map { day ->
        when (module) {
            HistoryModule.CANNABIS -> day.cannabisUses.toFloat()
            HistoryModule.TOBACCO -> day.tobaccoUses.toFloat()
            HistoryModule.ALCOHOL -> day.alcoholStandardDrinks.toFloat()
            HistoryModule.LIBIDO -> day.libidoEvents.toFloat()
            HistoryModule.SLEEP -> day.sleepHours ?: 0f
        }
    }
}

@Composable
fun ModuleHistoryScreen(module: HistoryModule, onBack: () -> Unit) {
    val viewModel = dopaMindViewModel { container -> ModuleHistoryViewModel(module, container.correlationEngine) }
    val days by viewModel.days.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(titleRes = historyTitleRes(module), onBack = onBack) }
        item {
            val dayList = days
            if (dayList == null) {
                Text(stringResource(R.string.recap_loading), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            } else {
                val values = viewModel.valuesFor(dayList)
                val total = values.sum()
                val average = if (values.isNotEmpty()) total / values.size else 0f

                DMCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.history_window_caption, HISTORY_DAYS),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    SimpleBarChart(values = values)
                    Spacer(Modifier.height(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.history_total, total),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Accent,
                        )
                        Text(
                            text = stringResource(R.string.history_average, average),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                        )
                    }
                }
            }
        }
    }
}

private fun historyTitleRes(module: HistoryModule): Int = when (module) {
    HistoryModule.CANNABIS -> R.string.module_cannabis
    HistoryModule.TOBACCO -> R.string.module_tobacco
    HistoryModule.ALCOHOL -> R.string.module_alcohol
    HistoryModule.LIBIDO -> R.string.module_libido
    HistoryModule.SLEEP -> R.string.recovery_sleep_title
}
