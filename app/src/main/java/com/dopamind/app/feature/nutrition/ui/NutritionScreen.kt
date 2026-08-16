package com.dopamind.app.feature.nutrition.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.DMChip
import com.dopamind.app.core.designsystem.DMPrimaryButton
import com.dopamind.app.core.designsystem.DMTextField
import com.dopamind.app.core.designsystem.ProgressRing
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.core.designsystem.SectionHeader
import com.dopamind.app.core.designsystem.SimpleBarChart
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.BorderSubtle
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.feature.nutrition.data.FastingSessionEntity
import com.dopamind.app.feature.nutrition.data.FoodLogEntity
import com.dopamind.app.feature.nutrition.data.MealType
import com.dopamind.app.feature.nutrition.domain.NutritionGoal

private val FASTING_HOUR_OPTIONS = listOf(12, 14, 16, 18, 20, 24)
private val WATER_QUICK_ADD_ML = listOf(250, 500, 1000)
private val WEIGHT_QUICK_OPTIONS = listOf(50f, 60f, 70f, 80f, 90f, 100f)

@Composable
fun NutritionScreen(onBack: () -> Unit) {
    val viewModel = dopaMindViewModel { container -> NutritionViewModel(container.nutritionRepository, container.profileRepository) }
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val todayFoodLogs by viewModel.todayFoodLogs.collectAsStateWithLifecycle()
    val todayWaterMl by viewModel.todayWaterMl.collectAsStateWithLifecycle()
    val recentWeightLogs by viewModel.recentWeightLogs.collectAsStateWithLifecycle()
    val activeFasting by viewModel.activeFasting.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(titleRes = R.string.module_nutrition, onBack = onBack) }
        item { DailySummaryCard(goal, todayFoodLogs) }
        item { MealLogCard(onLog = viewModel::logFood) }
        item { SectionHeader(stringResource(R.string.nutrition_today_meals)) }
        items(todayFoodLogs) { log -> FoodRow(log, onDelete = { viewModel.deleteFood(log.id) }) }
        item { WaterCard(todayWaterMl, profile?.dailyWaterGoalMl ?: 2000, onAddWater = viewModel::logWater) }
        item { WeightCard(recentWeightLogs.map { it.weightKg }, onLogWeight = viewModel::logWeight) }
        item { FastingCard(activeFasting, onStart = viewModel::startFasting, onEnd = viewModel::endFasting) }
    }
}

@Composable
private fun DailySummaryCard(goal: NutritionGoal?, todayFoodLogs: List<FoodLogEntity>) {
    val consumedCalories = todayFoodLogs.sumOf { it.calories }
    val consumedProtein = todayFoodLogs.sumOf { it.proteinGrams.toDouble() }.toFloat()
    val consumedCarbs = todayFoodLogs.sumOf { it.carbsGrams.toDouble() }.toFloat()
    val consumedFat = todayFoodLogs.sumOf { it.fatGrams.toDouble() }.toFloat()

    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.nutrition_summary_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        if (goal == null) {
            Text(stringResource(R.string.recap_loading), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProgressRing(progress = (consumedCalories.toFloat() / goal.calories).coerceIn(0f, 1f)) {
                    Text("$consumedCalories", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                }
                Column {
                    Text(
                        text = stringResource(R.string.nutrition_calories_goal, consumedCalories, goal.calories),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Accent,
                    )
                    Text(
                        text = stringResource(R.string.nutrition_macros_line, consumedProtein.toInt(), goal.proteinGrams, consumedCarbs.toInt(), goal.carbsGrams, consumedFat.toInt(), goal.fatGrams),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MealLogCard(onLog: (MealType, String, Int, Float, Float, Float) -> Unit) {
    var mealType by remember { mutableStateOf(MealType.BREAKFAST) }
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.nutrition_log_food_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MealType.entries.toList()) { m ->
                DMChip(label = mealTypeLabel(m), selected = mealType == m, onClick = { mealType = m })
            }
        }
        Spacer(Modifier.height(12.dp))
        DMTextField(value = name, onValueChange = { name = it }, placeholder = stringResource(R.string.nutrition_food_name_placeholder), singleLine = true)
        Spacer(Modifier.height(8.dp))
        DMTextField(value = calories, onValueChange = { calories = it }, placeholder = stringResource(R.string.nutrition_calories_placeholder), singleLine = true)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DMTextField(value = protein, onValueChange = { protein = it }, placeholder = stringResource(R.string.nutrition_protein_placeholder), singleLine = true, modifier = Modifier.weight(1f))
            DMTextField(value = carbs, onValueChange = { carbs = it }, placeholder = stringResource(R.string.nutrition_carbs_placeholder), singleLine = true, modifier = Modifier.weight(1f))
            DMTextField(value = fat, onValueChange = { fat = it }, placeholder = stringResource(R.string.nutrition_fat_placeholder), singleLine = true, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        DMPrimaryButton(
            text = stringResource(R.string.nutrition_log_food_save),
            onClick = {
                val caloriesValue = calories.toIntOrNull()
                if (name.isNotBlank() && caloriesValue != null) {
                    onLog(mealType, name, caloriesValue, protein.toFloatOrNull() ?: 0f, carbs.toFloatOrNull() ?: 0f, fat.toFloatOrNull() ?: 0f)
                    name = ""; calories = ""; protein = ""; carbs = ""; fat = ""
                }
            },
        )
    }
}

@Composable
private fun mealTypeLabel(mealType: MealType): String = stringResource(
    when (mealType) {
        MealType.BREAKFAST -> R.string.nutrition_meal_breakfast
        MealType.LUNCH -> R.string.nutrition_meal_lunch
        MealType.DINNER -> R.string.nutrition_meal_dinner
        MealType.SNACK -> R.string.nutrition_meal_snack
    }
)

@Composable
private fun FoodRow(log: FoodLogEntity, onDelete: () -> Unit) {
    DMCard(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = log.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Text(text = mealTypeLabel(MealType.valueOf(log.mealType)), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
            Text(text = stringResource(R.string.nutrition_calories_short, log.calories), style = MaterialTheme.typography.bodyLarge, color = Accent)
        }
    }
}

@Composable
private fun WaterCard(todayWaterMl: Int, goalMl: Int, onAddWater: (Int) -> Unit) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.nutrition_water_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ProgressRing(progress = (todayWaterMl.toFloat() / goalMl).coerceIn(0f, 1f)) {
                Text(stringResource(R.string.nutrition_water_ml, todayWaterMl / 1000f), style = MaterialTheme.typography.labelMedium, color = TextPrimary)
            }
            Text(stringResource(R.string.nutrition_water_goal, goalMl / 1000f), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WATER_QUICK_ADD_ML.forEach { amount ->
                DMChip(label = "+${amount}ml", selected = false, onClick = { onAddWater(amount) })
            }
        }
    }
}

@Composable
private fun WeightCard(recentWeights: List<Float>, onLogWeight: (Float) -> Unit) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.nutrition_weight_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WEIGHT_QUICK_OPTIONS.forEach { w ->
                DMChip(label = "${w.toInt()}kg", selected = false, onClick = { onLogWeight(w) })
            }
        }
        if (recentWeights.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SimpleBarChart(values = recentWeights, barColor = Accent, trackColor = BorderSubtle)
        }
    }
}

@Composable
private fun FastingCard(active: FastingSessionEntity?, onStart: (Int) -> Unit, onEnd: (Boolean) -> Unit) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.nutrition_fasting_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        if (active == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FASTING_HOUR_OPTIONS.forEach { h ->
                    DMChip(label = "${h}h", selected = false, onClick = { onStart(h) })
                }
            }
        } else {
            val elapsedHours = (System.currentTimeMillis() - active.startEpochMillis) / 3_600_000f
            val progress = (elapsedHours / active.targetHours).coerceIn(0f, 1f)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProgressRing(progress = progress) {
                    Text(stringResource(R.string.nutrition_fasting_hours, elapsedHours), style = MaterialTheme.typography.labelMedium, color = Accent)
                }
                Column {
                    Text(
                        text = stringResource(R.string.nutrition_fasting_target, active.targetHours),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    DMPrimaryButton(text = stringResource(R.string.nutrition_fasting_end), onClick = { onEnd(progress >= 1f) })
                }
            }
        }
    }
}
