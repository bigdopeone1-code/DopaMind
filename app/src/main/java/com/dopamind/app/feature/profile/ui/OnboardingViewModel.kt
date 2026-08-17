package com.dopamind.app.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopamind.app.core.i18n.LocaleController
import com.dopamind.app.core.notifications.NotificationScheduler
import com.dopamind.app.feature.alcohol.domain.BiologicalSex
import com.dopamind.app.feature.profile.data.ActivityLevel
import com.dopamind.app.feature.profile.data.LanguagePreference
import com.dopamind.app.feature.profile.data.NutritionGoalType
import com.dopamind.app.feature.profile.data.ProfileRepository
import com.dopamind.app.feature.profile.data.SmokingStatus
import com.dopamind.app.feature.profile.data.UseFrequency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val pageIndex: Int = 0,
    val displayName: String = "",
    val weightKg: Float = 70f,
    val heightCm: Int = 170,
    val sex: BiologicalSex = BiologicalSex.OTHER,
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,
    val goalType: NutritionGoalType = NutritionGoalType.MAINTAIN,
    val targetWeightKg: Float? = null,
    val smokingStatus: SmokingStatus = SmokingStatus.NEVER,
    val cannabisUseFrequency: UseFrequency = UseFrequency.NEVER,
    val alcoholUseFrequency: UseFrequency = UseFrequency.NEVER,
    val language: LanguagePreference = LanguagePreference.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val dailyReminderHour: Int = 20,
    val completed: Boolean = false,
)

class OnboardingViewModel(
    private val profileRepository: ProfileRepository,
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onPageChange(page: Int) = _uiState.update { it.copy(pageIndex = page) }
    fun onNameChange(name: String) = _uiState.update { it.copy(displayName = name) }
    fun onWeightChange(weightKg: Float) = _uiState.update { it.copy(weightKg = weightKg) }
    fun onHeightChange(heightCm: Int) = _uiState.update { it.copy(heightCm = heightCm) }
    fun onSexChange(sex: BiologicalSex) = _uiState.update { it.copy(sex = sex) }
    fun onActivityLevelChange(level: ActivityLevel) = _uiState.update { it.copy(activityLevel = level) }
    fun onGoalTypeChange(goalType: NutritionGoalType) = _uiState.update {
        // A new goal invalidates any previously picked target weight — MAINTAIN has none.
        it.copy(goalType = goalType, targetWeightKg = if (goalType == NutritionGoalType.MAINTAIN) null else it.targetWeightKg)
    }
    fun onTargetWeightChange(targetWeightKg: Float) = _uiState.update { it.copy(targetWeightKg = targetWeightKg) }
    fun onSmokingStatusChange(status: SmokingStatus) = _uiState.update { it.copy(smokingStatus = status) }
    fun onCannabisUseChange(frequency: UseFrequency) = _uiState.update { it.copy(cannabisUseFrequency = frequency) }
    fun onAlcoholUseChange(frequency: UseFrequency) = _uiState.update { it.copy(alcoholUseFrequency = frequency) }
    fun onLanguageChange(language: LanguagePreference) = _uiState.update { it.copy(language = language) }
    fun onNotificationsEnabledChange(enabled: Boolean) = _uiState.update { it.copy(notificationsEnabled = enabled) }
    fun onReminderHourChange(hour: Int) = _uiState.update { it.copy(dailyReminderHour = hour) }

    fun finish() {
        val state = _uiState.value
        LocaleController.apply(state.language)
        viewModelScope.launch {
            profileRepository.completeOnboarding(
                displayName = state.displayName.ifBlank { "" },
                weightKg = state.weightKg,
                sex = state.sex,
                language = state.language,
                notificationsEnabled = state.notificationsEnabled,
                dailyReminderHour = state.dailyReminderHour,
                heightCm = state.heightCm,
                smokingStatus = state.smokingStatus,
                cannabisUseFrequency = state.cannabisUseFrequency,
                alcoholUseFrequency = state.alcoholUseFrequency,
                activityLevel = state.activityLevel,
                nutritionGoalType = state.goalType,
                targetWeightKg = state.targetWeightKg,
            )
            if (state.notificationsEnabled) {
                notificationScheduler.scheduleDailyCheckInReminder(state.dailyReminderHour)
                notificationScheduler.scheduleCravingAlerts()
            }
            _uiState.update { it.copy(completed = true) }
        }
    }
}
