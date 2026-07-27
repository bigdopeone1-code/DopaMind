package com.dopamind.app.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopamind.app.core.i18n.LocaleController
import com.dopamind.app.core.notifications.NotificationScheduler
import com.dopamind.app.feature.alcohol.domain.BiologicalSex
import com.dopamind.app.feature.profile.data.LanguagePreference
import com.dopamind.app.feature.profile.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val pageIndex: Int = 0,
    val displayName: String = "",
    val weightKg: Float = 70f,
    val sex: BiologicalSex = BiologicalSex.OTHER,
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
    fun onSexChange(sex: BiologicalSex) = _uiState.update { it.copy(sex = sex) }
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
            )
            if (state.notificationsEnabled) {
                notificationScheduler.scheduleDailyCheckInReminder(state.dailyReminderHour)
                notificationScheduler.scheduleCravingAlerts()
            }
            _uiState.update { it.copy(completed = true) }
        }
    }
}
