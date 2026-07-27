package com.dopamind.app.feature.profile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.DMChip
import com.dopamind.app.core.designsystem.DMSecondaryButton
import com.dopamind.app.core.designsystem.DMTextField
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.Danger
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.feature.alcohol.domain.BiologicalSex
import com.dopamind.app.feature.profile.data.LanguagePreference

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val viewModel = dopaMindViewModel { container ->
        ProfileViewModel(container.profileRepository, container.notificationScheduler, container.backupManager)
    }
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let(viewModel::exportBackup)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importBackup)
    }

    val currentProfile = profile
    if (currentProfile == null) {
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(titleRes = R.string.profile_title, onBack = onBack) }

        item {
            DMCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_name_label), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                DMTextField(value = currentProfile.displayName, onValueChange = viewModel::updateName, placeholder = stringResource(R.string.onboarding_name_placeholder), singleLine = true)
            }
        }

        item {
            DMCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_body_label), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(50f, 60f, 70f, 80f, 90f, 100f).forEach { w ->
                        DMChip(label = "${w.toInt()}kg", selected = currentProfile.weightKg == w, onClick = { viewModel.updateWeight(w) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BiologicalSex.entries.forEach { s ->
                        DMChip(
                            label = profileSexLabel(s),
                            selected = currentProfile.biologicalSex == s.name,
                            onClick = { viewModel.updateSex(s) },
                        )
                    }
                }
            }
        }

        item {
            DMCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_language_title), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguagePreference.entries.forEach { lang ->
                        DMChip(
                            label = profileLanguageLabel(lang),
                            selected = currentProfile.languagePreference == lang.name,
                            onClick = { viewModel.updateLanguage(lang) },
                        )
                    }
                }
            }
        }

        item {
            DMCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_notifications_label), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                DMChip(
                    label = stringResource(if (currentProfile.notificationsEnabled) R.string.onboarding_notifications_on else R.string.onboarding_notifications_off),
                    selected = currentProfile.notificationsEnabled,
                    onClick = { viewModel.updateNotificationsEnabled(!currentProfile.notificationsEnabled) },
                )
                if (currentProfile.notificationsEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(9, 12, 18, 20, 22).forEach { h ->
                            DMChip(label = "${h}:00", selected = currentProfile.dailyReminderHour == h, onClick = { viewModel.updateReminderHour(h) })
                        }
                    }
                }
            }
        }

        item {
            DMCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_backup_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(stringResource(R.string.profile_backup_subtitle), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DMSecondaryButton(
                        text = stringResource(R.string.profile_backup_export),
                        onClick = { exportLauncher.launch("dopamind_backup.db") },
                        modifier = Modifier.weight(1f),
                    )
                    DMSecondaryButton(
                        text = stringResource(R.string.profile_backup_import),
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                    )
                }
                when (backupStatus) {
                    BackupStatus.EXPORT_SUCCESS -> BackupMessage(stringResource(R.string.profile_backup_export_success), Accent)
                    BackupStatus.EXPORT_FAILED -> BackupMessage(stringResource(R.string.profile_backup_export_failed), Danger)
                    BackupStatus.IMPORT_SUCCESS -> BackupMessage(stringResource(R.string.profile_backup_import_success), Accent)
                    BackupStatus.IMPORT_FAILED -> BackupMessage(stringResource(R.string.profile_backup_import_failed), Danger)
                    BackupStatus.NONE -> Unit
                }
            }
        }
    }
}

@Composable
private fun BackupMessage(text: String, color: androidx.compose.ui.graphics.Color) {
    Spacer(Modifier.height(8.dp))
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
private fun profileSexLabel(sex: BiologicalSex): String = stringResource(
    when (sex) {
        BiologicalSex.MALE -> R.string.alcohol_sex_male
        BiologicalSex.FEMALE -> R.string.alcohol_sex_female
        BiologicalSex.OTHER -> R.string.alcohol_sex_other
    }
)

@Composable
private fun profileLanguageLabel(language: LanguagePreference): String = stringResource(
    when (language) {
        LanguagePreference.SYSTEM -> R.string.onboarding_language_system
        LanguagePreference.IT -> R.string.onboarding_language_it
        LanguagePreference.EN -> R.string.onboarding_language_en
    }
)
