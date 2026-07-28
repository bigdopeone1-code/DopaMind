package com.dopamind.app.feature.profile.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopamind.app.core.backup.BackupManager
import com.dopamind.app.core.backup.BackupResult
import com.dopamind.app.core.i18n.LocaleController
import com.dopamind.app.core.notifications.NotificationScheduler
import com.dopamind.app.feature.alcohol.domain.BiologicalSex
import com.dopamind.app.feature.profile.data.LanguagePreference
import com.dopamind.app.feature.profile.data.ProfileRepository
import com.dopamind.app.feature.profile.data.UserProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BackupStatus { NONE, EXPORT_SUCCESS, EXPORT_FAILED, IMPORT_SUCCESS, IMPORT_FAILED }

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val notificationScheduler: NotificationScheduler,
    private val backupManager: BackupManager,
) : ViewModel() {

    val profile: StateFlow<UserProfileEntity?> =
        profileRepository.observeProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _backupStatus = MutableStateFlow(BackupStatus.NONE)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    private fun update(mutate: (UserProfileEntity) -> UserProfileEntity) {
        val current = profile.value ?: return
        viewModelScope.launch { profileRepository.updateProfile(current, mutate) }
    }

    fun updateName(name: String) = update { it.copy(displayName = name) }
    fun updateWeight(weightKg: Float) = update { it.copy(weightKg = weightKg) }
    fun updateSex(sex: BiologicalSex) = update { it.copy(biologicalSex = sex.name) }
    fun updateLanguage(language: LanguagePreference) {
        update { it.copy(languagePreference = language.name) }
        LocaleController.apply(language)
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        update { it.copy(notificationsEnabled = enabled) }
        if (enabled) {
            val hour = profile.value?.dailyReminderHour ?: 20
            notificationScheduler.scheduleDailyCheckInReminder(hour)
            notificationScheduler.scheduleCravingAlerts()
        } else {
            notificationScheduler.cancelAll()
        }
    }

    fun updateImmersiveMode(enabled: Boolean) = update { it.copy(immersiveModeEnabled = enabled) }

    fun updateReminderHour(hour: Int) {
        update { it.copy(dailyReminderHour = hour) }
        if (profile.value?.notificationsEnabled == true) {
            notificationScheduler.scheduleDailyCheckInReminder(hour)
        }
    }

    fun exportBackup(destination: Uri) {
        viewModelScope.launch {
            val result = backupManager.export(destination)
            _backupStatus.value = if (result is BackupResult.Success) BackupStatus.EXPORT_SUCCESS else BackupStatus.EXPORT_FAILED
        }
    }

    fun importBackup(source: Uri) {
        viewModelScope.launch {
            val result = backupManager.import(source)
            _backupStatus.value = if (result is BackupResult.Success) BackupStatus.IMPORT_SUCCESS else BackupStatus.IMPORT_FAILED
        }
    }

    fun dismissBackupStatus() {
        _backupStatus.value = BackupStatus.NONE
    }
}
