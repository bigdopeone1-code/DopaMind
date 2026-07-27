package com.dopamind.app.feature.weeklyrecap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.dopamind.app.feature.weeklyrecap.domain.MostActiveModule

@Composable
fun AnnualWrappedScreen(onBack: () -> Unit) {
    val viewModel = dopaMindViewModel { container -> AnnualWrappedViewModel(container.annualWrappedGenerator) }
    val wrapped by viewModel.wrapped.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        val data = wrapped
        if (data == null) {
            Text(
                text = stringResource(R.string.recap_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = "🎉", fontSize = 40.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(
                    text = stringResource(R.string.wrapped_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                WrappedStatCard(stringResource(R.string.wrapped_total_spend, data.totalSpendEuros))
                WrappedStatCard(stringResource(R.string.wrapped_badges, data.badgesUnlocked))
                WrappedStatCard(stringResource(R.string.wrapped_smoke_free_streak, data.bestSmokeFreeStreakDays))
                data.averageSleepHours?.let { WrappedStatCard(stringResource(R.string.wrapped_avg_sleep, it)) }
                WrappedStatCard(stringResource(R.string.wrapped_most_active, stringResource(mostActiveModuleLabelRes(data.mostActiveModule))))
            }
        }

        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close), tint = TextSecondary)
        }
    }
}

@Composable
private fun WrappedStatCard(text: String) {
    DMCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = text, style = MaterialTheme.typography.titleLarge, color = Accent)
    }
}

private fun mostActiveModuleLabelRes(module: MostActiveModule): Int = when (module) {
    MostActiveModule.CANNABIS -> R.string.module_cannabis
    MostActiveModule.TOBACCO -> R.string.module_tobacco
    MostActiveModule.ALCOHOL -> R.string.module_alcohol
    MostActiveModule.LIBIDO -> R.string.module_libido
    MostActiveModule.NONE -> R.string.wrapped_most_active_none
}
