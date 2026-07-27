package com.dopamind.app.feature.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.SmokingRooms
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.SectionHeader
import com.dopamind.app.core.navigation.Destination
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary

private data class ModuleCardSpec(val destination: Destination, val icon: ImageVector, val titleRes: Int)

private val moduleCards = listOf(
    ModuleCardSpec(Destination.CannabisHome, Icons.Outlined.LocalFlorist, R.string.module_cannabis),
    ModuleCardSpec(Destination.TobaccoHome, Icons.Outlined.SmokingRooms, R.string.module_tobacco),
    ModuleCardSpec(Destination.AlcoholHome, Icons.Outlined.LocalBar, R.string.module_alcohol),
    ModuleCardSpec(Destination.LibidoHome, Icons.Outlined.Favorite, R.string.module_libido),
    ModuleCardSpec(Destination.DopamineFocusHome, Icons.Outlined.Bolt, R.string.module_dopamine_focus),
    ModuleCardSpec(Destination.RecoveryHome, Icons.Outlined.NightsStay, R.string.module_recovery),
    ModuleCardSpec(Destination.FinanceHome, Icons.Outlined.AccountBalanceWallet, R.string.module_finance),
)

@Composable
fun DashboardScreen(onNavigate: (Destination) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
            )
            Text(
                text = stringResource(R.string.dashboard_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
        }

        item {
            DMCard(onClick = { onNavigate(Destination.DailyVibeCheckIn) }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dashboard_checkin_cta_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Accent,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.dashboard_checkin_cta_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                QuickActionCard(
                    icon = Icons.Outlined.Mic,
                    label = stringResource(R.string.dashboard_quick_voice_log),
                    onClick = { onNavigate(Destination.VoiceLog) },
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    icon = Icons.Outlined.SupportAgent,
                    label = stringResource(R.string.dashboard_quick_sos),
                    onClick = { onNavigate(Destination.ChillCoachSos) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.dashboard_modules_header),
                trailing = stringResource(R.string.dashboard_weekly_recap_link),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(((moduleCards.size / 2 + moduleCards.size % 2) * 120).dp),
            ) {
                items(moduleCards) { spec ->
                    ModuleCard(spec, onClick = { onNavigate(spec.destination) })
                }
            }
        }

        item {
            DMCard(onClick = { onNavigate(Destination.WeeklyRecap) }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dashboard_weekly_recap_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                Text(
                    text = stringResource(R.string.dashboard_weekly_recap_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ModuleCard(spec: ModuleCardSpec, onClick: () -> Unit) {
    DMCard(onClick = onClick, modifier = Modifier.fillMaxWidth().height(108.dp)) {
        Icon(imageVector = spec.icon, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(spec.titleRes),
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
        )
    }
}

@Composable
private fun QuickActionCard(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DMCard(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
        }
    }
}
