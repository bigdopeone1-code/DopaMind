package com.dopamind.app.feature.voicelog.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.ai.voice.ParsedCategory
import com.dopamind.app.core.ai.voice.VoiceParseResult
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.DMPrimaryButton
import com.dopamind.app.core.designsystem.DMSecondaryButton
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary

@Composable
fun VoiceLogScreen(onBack: () -> Unit) {
    val viewModel = dopaMindViewModel { container ->
        VoiceLogViewModel(
            container.createVoiceCaptureManager(),
            container.cannabisRepository,
            container.tobaccoRepository,
            container.alcoholRepository,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) viewModel.startListening()
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val s = state) {
                is VoiceLogUiState.Idle -> IdleContent { if (hasPermission) viewModel.startListening() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                is VoiceLogUiState.Listening -> ListeningContent()
                is VoiceLogUiState.Parsed -> ParsedContent(s.result, onConfirm = { viewModel.confirmAndSave(s.result) }, onRetry = viewModel::reset)
                is VoiceLogUiState.Saved -> SavedContent(onDone = onBack)
                is VoiceLogUiState.Error -> ErrorContent(onRetry = viewModel::reset)
            }
        }

        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close), tint = TextSecondary)
        }
    }
}

@Composable
private fun IdleContent(onStart: () -> Unit) {
    Icon(Icons.Outlined.Mic, contentDescription = null, tint = Accent, modifier = Modifier.padding(bottom = 16.dp))
    Text(stringResource(R.string.voicelog_idle_title), style = MaterialTheme.typography.headlineLarge, color = TextPrimary, textAlign = TextAlign.Center)
    Text(stringResource(R.string.voicelog_idle_example), style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
    DMPrimaryButton(text = stringResource(R.string.voicelog_start), onClick = onStart)
}

@Composable
private fun ListeningContent() {
    Text(stringResource(R.string.voicelog_listening), style = MaterialTheme.typography.headlineLarge, color = Accent, textAlign = TextAlign.Center)
}

@Composable
private fun ParsedContent(result: VoiceParseResult, onConfirm: () -> Unit, onRetry: () -> Unit) {
    Text(stringResource(R.string.voicelog_transcript), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    Text(text = result.transcript, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.padding(bottom = 16.dp))

    if (result.items.isEmpty()) {
        Text(stringResource(R.string.voicelog_no_match), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
    } else {
        result.items.forEach { item ->
            DMCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(text = "${item.quantity}× ${stringResource(categoryLabel(item.category))}", style = MaterialTheme.typography.bodyLarge, color = Accent)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    if (result.items.isNotEmpty()) {
        DMPrimaryButton(text = stringResource(R.string.voicelog_confirm), onClick = onConfirm)
        Spacer(Modifier.height(8.dp))
    }
    DMSecondaryButton(text = stringResource(R.string.voicelog_retry), onClick = onRetry)
}

@Composable
private fun SavedContent(onDone: () -> Unit) {
    Text(stringResource(R.string.voicelog_saved), style = MaterialTheme.typography.headlineLarge, color = Accent, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    DMPrimaryButton(text = stringResource(R.string.checkin_done), onClick = onDone)
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Text(stringResource(R.string.voicelog_error), style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    DMSecondaryButton(text = stringResource(R.string.voicelog_retry), onClick = onRetry)
}

@Composable
private fun categoryLabel(category: ParsedCategory): Int = when (category) {
    ParsedCategory.CANNABIS -> R.string.module_cannabis
    ParsedCategory.TOBACCO -> R.string.module_tobacco
    ParsedCategory.ALCOHOL -> R.string.module_alcohol
}
