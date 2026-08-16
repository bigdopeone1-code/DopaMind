package com.dopamind.app.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMGlassCard
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.TextSecondary

private data class BottomTab(val destination: Destination, val icon: ImageVector, val labelRes: Int)

private val BottomTabs = listOf(
    BottomTab(Destination.Dashboard, Icons.Outlined.Home, R.string.tab_home),
    BottomTab(Destination.HistoryHub, Icons.Outlined.History, R.string.tab_history),
    BottomTab(Destination.VisionScanner(ScanTarget.DRINK_LABEL), Icons.Outlined.CameraAlt, R.string.tab_scan),
    BottomTab(Destination.WeeklyRecap, Icons.Outlined.Insights, R.string.tab_insights),
    BottomTab(Destination.Profile, Icons.Outlined.Person, R.string.tab_profile),
)

/** The 5 top-level destinations the floating bottom bar shows on. */
val BottomBarDestinationClasses = BottomTabs.map { it.destination::class }

@Composable
fun DopaMindBottomBar(currentDestination: NavDestination?, onNavigate: (Destination) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        DMGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                BottomTabs.forEach { tab ->
                    val selected = currentDestination?.hasRoute(tab.destination::class) == true
                    IconButton(onClick = { onNavigate(tab.destination) }) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.labelRes),
                            tint = if (selected) Accent else TextSecondary,
                        )
                    }
                }
            }
        }
    }
}
