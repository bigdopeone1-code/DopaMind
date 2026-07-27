package com.dopamind.app.feature.profile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dopamind.app.R
import com.dopamind.app.core.di.LocalAppContainer
import com.dopamind.app.core.theme.TextSecondary

/** Decides, once the profile finishes loading, whether onboarding is needed. */
@Composable
fun SplashScreen(onNeedsOnboarding: () -> Unit, onReady: () -> Unit) {
    val container = LocalAppContainer.current
    var decided by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val profile = container.profileRepository.getProfileOnce()
        decided = true
        if (profile == null) onNeedsOnboarding() else onReady()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!decided) {
            Text(text = stringResource(R.string.app_name), color = TextSecondary)
        }
    }
}
