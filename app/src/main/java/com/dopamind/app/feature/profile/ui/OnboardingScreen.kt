package com.dopamind.app.feature.profile.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMChip
import com.dopamind.app.core.designsystem.DMPrimaryButton
import com.dopamind.app.core.designsystem.DMTextField
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.feature.alcohol.domain.BiologicalSex
import com.dopamind.app.feature.profile.data.LanguagePreference
import kotlinx.coroutines.launch

private const val ONBOARDING_PAGE_COUNT = 4

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val viewModel = dopaMindViewModel { container ->
        OnboardingViewModel(container.profileRepository, container.notificationScheduler)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.completed) {
        if (uiState.completed) onComplete()
    }

    val pagerState = rememberPagerState(initialPage = 0) { ONBOARDING_PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(modifier = Modifier.fillMaxSize().padding(top = 56.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            when (page) {
                0 -> WelcomeStep(uiState.displayName, viewModel::onNameChange)
                1 -> ProfileStep(uiState.weightKg, viewModel::onWeightChange, uiState.sex, viewModel::onSexChange)
                2 -> LanguageStep(uiState.language, viewModel::onLanguageChange)
                else -> NotificationsStep(
                    enabled = uiState.notificationsEnabled,
                    onEnabledChange = { enabled ->
                        viewModel.onNotificationsEnabledChange(enabled)
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    hour = uiState.dailyReminderHour,
                    onHourChange = viewModel::onReminderHourChange,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        DMPrimaryButton(
            text = stringResource(if (pagerState.currentPage == ONBOARDING_PAGE_COUNT - 1) R.string.onboarding_finish else R.string.checkin_next),
            onClick = {
                if (pagerState.currentPage == ONBOARDING_PAGE_COUNT - 1) {
                    viewModel.finish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
        )
    }
}

@Composable
private fun WelcomeStep(name: String, onNameChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
        )
        DMTextField(value = name, onValueChange = onNameChange, placeholder = stringResource(R.string.onboarding_name_placeholder), singleLine = true)
    }
}

@Composable
private fun ProfileStep(weightKg: Float, onWeightChange: (Float) -> Unit, sex: BiologicalSex, onSexChange: (BiologicalSex) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.onboarding_profile_title),
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.onboarding_profile_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(50f, 60f, 70f, 80f, 90f, 100f)) { w ->
                DMChip(label = "${w.toInt()}kg", selected = weightKg == w, onClick = { onWeightChange(w) })
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BiologicalSex.entries.forEach { s ->
                DMChip(label = onboardingSexLabel(s), selected = sex == s, onClick = { onSexChange(s) })
            }
        }
    }
}

@Composable
private fun onboardingSexLabel(sex: BiologicalSex): String = stringResource(
    when (sex) {
        BiologicalSex.MALE -> R.string.alcohol_sex_male
        BiologicalSex.FEMALE -> R.string.alcohol_sex_female
        BiologicalSex.OTHER -> R.string.alcohol_sex_other
    }
)

@Composable
private fun LanguageStep(language: LanguagePreference, onLanguageChange: (LanguagePreference) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.onboarding_language_title),
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LanguagePreference.entries.forEach { lang ->
                DMChip(label = languageLabel(lang), selected = language == lang, onClick = { onLanguageChange(lang) })
            }
        }
    }
}

@Composable
private fun languageLabel(language: LanguagePreference): String = stringResource(
    when (language) {
        LanguagePreference.SYSTEM -> R.string.onboarding_language_system
        LanguagePreference.IT -> R.string.onboarding_language_it
        LanguagePreference.EN -> R.string.onboarding_language_en
    }
)

@Composable
private fun NotificationsStep(enabled: Boolean, onEnabledChange: (Boolean) -> Unit, hour: Int, onHourChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(R.string.onboarding_notifications_title),
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.onboarding_notifications_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
        )
        DMChip(
            label = stringResource(if (enabled) R.string.onboarding_notifications_on else R.string.onboarding_notifications_off),
            selected = enabled,
            onClick = { onEnabledChange(!enabled) },
        )
        if (enabled) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.onboarding_reminder_hour), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(9, 12, 18, 20, 22).forEach { h ->
                    DMChip(label = "${h}:00", selected = hour == h, onClick = { onHourChange(h) })
                }
            }
        }
    }
}
