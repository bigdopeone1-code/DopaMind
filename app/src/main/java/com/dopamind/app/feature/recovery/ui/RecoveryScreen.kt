package com.dopamind.app.feature.recovery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.dopamind.app.core.designsystem.PresetOption
import com.dopamind.app.core.designsystem.QuickPresetChipRow
import com.dopamind.app.core.designsystem.SectionHeader
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.BorderSubtle
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.feature.recovery.data.FoodCategory
import com.dopamind.app.feature.recovery.data.MunchiesLogEntity
import com.dopamind.app.feature.recovery.data.MunchiesSource
import com.dopamind.app.feature.recovery.data.RecoveryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class RecoveryViewModel(private val repository: RecoveryRepository) : ViewModel() {
    val munchiesLogs: StateFlow<List<MunchiesLogEntity>> =
        repository.observeMunchiesLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logSleep(hours: Float, quality: Int) {
        val todayEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        viewModelScope.launch { repository.logSleep(todayEpochDay, hours, quality, note = null) }
    }

    fun logMunchies(foodDescription: String, category: FoodCategory, junkScore: Int) {
        viewModelScope.launch { repository.logMunchies(foodDescription, junkScore, MunchiesSource.MANUAL, category) }
    }
}

@Composable
fun RecoveryScreen(onBack: () -> Unit, onSos: () -> Unit, onScanFood: () -> Unit, onViewSleepHistory: () -> Unit) {
    val viewModel = dopaMindViewModel { container -> RecoveryViewModel(container.recoveryRepository) }
    val munchiesLogs by viewModel.munchiesLogs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(titleRes = R.string.module_recovery, onBack = onBack) }
        item { DMSecondaryButton(text = stringResource(R.string.history_view_trend), onClick = onViewSleepHistory) }
        item { SosCard(onSos) }
        item { SleepCard(onLog = viewModel::logSleep) }
        item { MunchiesCard(onLogManual = viewModel::logMunchies, onScanFood = onScanFood) }
        item { SectionHeader(stringResource(R.string.recovery_recent_munchies)) }
        items(munchiesLogs.take(10)) { log -> MunchiesRow(log) }
    }
}

@Composable
private fun SosCard(onSos: () -> Unit) {
    DMCard(onClick = onSos, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.recovery_sos_title), style = MaterialTheme.typography.titleLarge, color = Accent)
        Text(stringResource(R.string.recovery_sos_subtitle), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
    }
}

@Composable
private fun SleepCard(onLog: (Float, Int) -> Unit) {
    var hours by remember { mutableStateOf(7f) }
    var quality by remember { mutableStateOf(3) }
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.recovery_sleep_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.checkin_sleep_hours_value, hours.toInt()), style = MaterialTheme.typography.headlineLarge, color = Accent)
        Slider(
            value = hours,
            onValueChange = { hours = it },
            valueRange = 0f..12f,
            steps = 11,
            colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = BorderSubtle),
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.recovery_sleep_quality), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { level -> DMChip(label = level.toString(), selected = quality == level, onClick = { quality = level }) }
        }
        Spacer(Modifier.height(12.dp))
        DMPrimaryButton(text = stringResource(R.string.recovery_sleep_save), onClick = { onLog(hours, quality) })
    }
}

@Composable
private fun foodCategoryLabel(category: FoodCategory): String = stringResource(
    when (category) {
        FoodCategory.SWEET -> R.string.food_category_sweet
        FoodCategory.SALTY_SNACK -> R.string.food_category_salty_snack
        FoodCategory.FRIED -> R.string.food_category_fried
        FoodCategory.FAST_FOOD -> R.string.food_category_fast_food
        FoodCategory.FRUIT_VEG -> R.string.food_category_fruit_veg
        FoodCategory.OTHER -> R.string.food_category_other
    }
)

@Composable
private fun MunchiesCard(onLogManual: (String, FoodCategory, Int) -> Unit, onScanFood: () -> Unit) {
    val options = FoodCategory.entries.map { PresetOption(it.name, foodCategoryLabel(it)) }
    var selectedCategory by remember { mutableStateOf(FoodCategory.entries.first()) }
    val selectedLabel = foodCategoryLabel(selectedCategory)
    var junkScore by remember { mutableStateOf(50f) }
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.recovery_munchies_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        QuickPresetChipRow(
            options = options,
            selectedId = selectedCategory.name,
            onSelect = { selectedCategory = FoodCategory.valueOf(it.id) },
        )
        Spacer(Modifier.height(8.dp))
        Slider(
            value = junkScore,
            onValueChange = { junkScore = it },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = BorderSubtle),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DMPrimaryButton(
                text = stringResource(R.string.recovery_munchies_save),
                onClick = { onLogManual(selectedLabel, selectedCategory, junkScore.toInt()) },
                modifier = Modifier.weight(1f),
            )
            DMSecondaryButton(text = stringResource(R.string.recovery_munchies_scan), onClick = onScanFood, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MunchiesRow(log: MunchiesLogEntity) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = log.foodDescription, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        Text(text = stringResource(R.string.recovery_munchies_score, log.junkScore), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}
