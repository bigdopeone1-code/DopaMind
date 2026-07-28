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
import com.dopamind.app.core.designsystem.BarGroup
import com.dopamind.app.core.designsystem.BarValue
import com.dopamind.app.core.designsystem.BreakdownBarChart
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.core.designsystem.SectionHeader
import com.dopamind.app.core.designsystem.SimpleBarChart
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.navigation.HistoryModule
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.BorderHover
import com.dopamind.app.core.theme.ChartPalette
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.feature.alcohol.data.AlcoholRepository
import com.dopamind.app.feature.alcohol.data.DrinkType
import com.dopamind.app.feature.cannabis.data.CannabisRepository
import com.dopamind.app.feature.cannabis.data.ConsumptionMethod
import com.dopamind.app.feature.tobacco.data.TobaccoProductType
import com.dopamind.app.feature.tobacco.data.TobaccoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val HISTORY_DAYS = 30

class ModuleHistoryViewModel(
    private val module: HistoryModule,
    private val correlationEngine: CorrelationEngine,
    private val cannabisRepository: CannabisRepository,
    private val alcoholRepository: AlcoholRepository,
    private val tobaccoRepository: TobaccoRepository,
) : ViewModel() {
    private val _days = MutableStateFlow<List<DailyMetrics>?>(null)
    val days: StateFlow<List<DailyMetrics>?> = _days.asStateFlow()

    private val _categoryBreakdown = MutableStateFlow<Map<String, Int>?>(null)
    val categoryBreakdown: StateFlow<Map<String, Int>?> = _categoryBreakdown.asStateFlow()

    init {
        viewModelScope.launch { _days.value = correlationEngine.aggregateDailyMetrics(HISTORY_DAYS) }
        viewModelScope.launch {
            val sinceMillis = System.currentTimeMillis() - HISTORY_DAYS.toLong() * 86_400_000L
            _categoryBreakdown.value = when (module) {
                HistoryModule.CANNABIS -> cannabisRepository.observeLogsSince(sinceMillis).first()
                    .groupingBy { it.method }.eachCount()
                HistoryModule.ALCOHOL -> alcoholRepository.observeLogsSince(sinceMillis).first()
                    .groupingBy { it.drinkType }.eachCount()
                HistoryModule.TOBACCO -> tobaccoRepository.observeLogsSince(sinceMillis).first()
                    .groupingBy { it.productType }.eachCount()
                HistoryModule.LIBIDO, HistoryModule.SLEEP -> null
            }
        }
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
    val viewModel = dopaMindViewModel { container ->
        ModuleHistoryViewModel(
            module,
            container.correlationEngine,
            container.cannabisRepository,
            container.alcoholRepository,
            container.tobaccoRepository,
        )
    }
    val days by viewModel.days.collectAsStateWithLifecycle()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsStateWithLifecycle()

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
        val breakdown = categoryBreakdown
        if (breakdown != null && breakdown.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.history_breakdown_header)) }
            item {
                DMCard(modifier = Modifier.fillMaxWidth()) {
                    val entries = breakdown.entries.toList()
                    val groups = entries.mapIndexed { index, (rawName, count) ->
                        BarGroup(
                            label = categoryLabel(module, rawName),
                            bars = listOf(BarValue(count.toFloat(), ChartPalette[index % ChartPalette.size])),
                        )
                    }
                    BreakdownBarChart(groups = groups)
                }
            }
        }

        val dayListForWeek = days
        if (dayListForWeek != null && dayListForWeek.size >= 14) {
            item { SectionHeader(stringResource(R.string.history_week_over_week_header)) }
            item {
                DMCard(modifier = Modifier.fillMaxWidth()) {
                    val values = viewModel.valuesFor(dayListForWeek)
                    val last14Days = dayListForWeek.takeLast(14)
                    val last14Values = values.takeLast(14)
                    val lastWeekValues = last14Values.take(7)
                    val thisWeekDays = last14Days.takeLast(7)
                    val thisWeekValues = last14Values.takeLast(7)
                    val groups = thisWeekDays.mapIndexed { index, day ->
                        val label = LocalDate.ofEpochDay(day.epochDay).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        BarGroup(
                            label = label,
                            bars = listOf(
                                BarValue(lastWeekValues.getOrElse(index) { 0f }, BorderHover),
                                BarValue(thisWeekValues.getOrElse(index) { 0f }, Accent),
                            ),
                        )
                    }
                    BreakdownBarChart(groups = groups)
                }
            }
        }
    }
}

@Composable
private fun categoryLabel(module: HistoryModule, rawName: String): String = when (module) {
    HistoryModule.CANNABIS -> runCatching { ConsumptionMethod.valueOf(rawName) }.getOrNull()?.let {
        stringResource(
            when (it) {
                ConsumptionMethod.JOINT -> R.string.cannabis_method_joint
                ConsumptionMethod.VAPE -> R.string.cannabis_method_vape
                ConsumptionMethod.EDIBLE -> R.string.cannabis_method_edible
                ConsumptionMethod.TINCTURE -> R.string.cannabis_method_tincture
                ConsumptionMethod.BONG -> R.string.cannabis_method_bong
                ConsumptionMethod.OTHER -> R.string.cannabis_method_other
            }
        )
    } ?: rawName
    HistoryModule.ALCOHOL -> runCatching { DrinkType.valueOf(rawName) }.getOrNull()?.let {
        stringResource(
            when (it) {
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
    } ?: rawName
    HistoryModule.TOBACCO -> runCatching { TobaccoProductType.valueOf(rawName) }.getOrNull()?.let {
        stringResource(
            when (it) {
                TobaccoProductType.CIGARETTE -> R.string.tobacco_product_cigarette
                TobaccoProductType.IQOS_STICK -> R.string.tobacco_product_iqos
                TobaccoProductType.VAPE_PUFF -> R.string.tobacco_product_vape
                TobaccoProductType.OTHER -> R.string.tobacco_product_other
            }
        )
    } ?: rawName
    HistoryModule.LIBIDO, HistoryModule.SLEEP -> rawName
}

private fun historyTitleRes(module: HistoryModule): Int = when (module) {
    HistoryModule.CANNABIS -> R.string.module_cannabis
    HistoryModule.TOBACCO -> R.string.module_tobacco
    HistoryModule.ALCOHOL -> R.string.module_alcohol
    HistoryModule.LIBIDO -> R.string.module_libido
    HistoryModule.SLEEP -> R.string.recovery_sleep_title
}
