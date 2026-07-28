package com.dopamind.app.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.ScreenHeader
import com.dopamind.app.core.navigation.HistoryModule
import com.dopamind.app.core.theme.TextPrimary

@Composable
fun HistoryHubScreen(onBack: () -> Unit, onSelectModule: (HistoryModule) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader(titleRes = R.string.history_hub_title, onBack = onBack) }
        items(HistoryModule.entries) { module ->
            DMCard(onClick = { onSelectModule(module) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(historyModuleLabel(module)), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
        }
    }
}

private fun historyModuleLabel(module: HistoryModule): Int = when (module) {
    HistoryModule.CANNABIS -> R.string.module_cannabis
    HistoryModule.TOBACCO -> R.string.module_tobacco
    HistoryModule.ALCOHOL -> R.string.module_alcohol
    HistoryModule.LIBIDO -> R.string.module_libido
    HistoryModule.SLEEP -> R.string.recovery_sleep_title
}
