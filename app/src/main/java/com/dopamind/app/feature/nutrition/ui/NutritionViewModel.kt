package com.dopamind.app.feature.nutrition.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopamind.app.feature.alcohol.domain.BiologicalSex
import com.dopamind.app.feature.nutrition.data.FastingSessionEntity
import com.dopamind.app.feature.nutrition.data.FoodLogEntity
import com.dopamind.app.feature.nutrition.data.MealType
import com.dopamind.app.feature.nutrition.data.NutritionRepository
import com.dopamind.app.feature.nutrition.data.WeightLogEntity
import com.dopamind.app.feature.nutrition.domain.NutritionGoal
import com.dopamind.app.feature.nutrition.domain.NutritionGoalCalculator
import com.dopamind.app.feature.profile.data.ActivityLevel
import com.dopamind.app.feature.profile.data.ProfileRepository
import com.dopamind.app.feature.profile.data.UserProfileEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private const val WEIGHT_HISTORY_DAYS = 30L

class NutritionViewModel(
    private val repository: NutritionRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private fun todayStartMillis() =
        LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val profile: StateFlow<UserProfileEntity?> =
        profileRepository.observeProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val goal: StateFlow<NutritionGoal?> = profile.map { p ->
        p?.let {
            NutritionGoalCalculator.calculateGoal(
                weightKg = it.weightKg,
                heightCm = it.heightCm,
                ageYears = it.ageYears,
                sex = runCatching { BiologicalSex.valueOf(it.biologicalSex) }.getOrDefault(BiologicalSex.OTHER),
                activityLevel = runCatching { ActivityLevel.valueOf(it.activityLevel) }.getOrDefault(ActivityLevel.LIGHT),
                manualCalorieOverride = it.dailyCalorieGoalOverride,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayFoodLogs: StateFlow<List<FoodLogEntity>> =
        repository.observeFoodLogsSince(todayStartMillis()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayWaterMl: StateFlow<Int> = repository.observeWaterLogsSince(todayStartMillis())
        .map { logs -> logs.sumOf { it.amountMl } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentWeightLogs: StateFlow<List<WeightLogEntity>> =
        repository.observeWeightLogsSince(LocalDate.now(ZoneId.systemDefault()).toEpochDay() - WEIGHT_HISTORY_DAYS)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeFasting: StateFlow<FastingSessionEntity?> =
        repository.observeActiveFastingSession().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logFood(mealType: MealType, name: String, calories: Int, proteinGrams: Float, carbsGrams: Float, fatGrams: Float) {
        viewModelScope.launch { repository.logFood(mealType, name, calories, proteinGrams, carbsGrams, fatGrams) }
    }

    fun deleteFood(id: Long) = viewModelScope.launch { repository.deleteFood(id) }

    fun logWater(amountMl: Int) = viewModelScope.launch { repository.logWater(amountMl) }

    fun logWeight(weightKg: Float) {
        val todayEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        viewModelScope.launch { repository.logWeight(todayEpochDay, weightKg) }
    }

    fun startFasting(targetHours: Int) = viewModelScope.launch { repository.startFasting(targetHours) }

    fun endFasting(completed: Boolean) = viewModelScope.launch {
        activeFasting.value?.let { repository.endFasting(it, completed) }
    }
}
