package com.dopamind.app.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMGlassCard
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.BackgroundPrimary
import com.dopamind.app.core.theme.TextSecondary

private data class BottomTab(val destination: Destination, val icon: ImageVector, val labelRes: Int)

/**
 * The four top-level surfaces. The central "+" is deliberately *not* one of
 * them: it is an action, not a place, so it neither shows a selected state nor
 * keeps the user there once the log is saved.
 */
private val LeftTabs = listOf(
    BottomTab(Destination.Home, Icons.Outlined.Home, R.string.tab_home),
    BottomTab(Destination.Trends, Icons.Outlined.ShowChart, R.string.tab_trends),
)

private val RightTabs = listOf(
    BottomTab(Destination.Insights, Icons.Outlined.Insights, R.string.tab_insights),
    BottomTab(Destination.Profile, Icons.Outlined.Person, R.string.tab_profile),
)

/** The destinations the floating bottom bar shows on. */
val BottomBarDestinationClasses = (LeftTabs + RightTabs).map { it.destination::class }

@Composable
fun DopaMindBottomBar(currentDestination: NavDestination?, onNavigate: (Destination) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        DMGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LeftTabs.forEach { TabIcon(it, currentDestination, onNavigate) }
                AddEventButton(onClick = { onNavigate(Destination.AddEvent()) })
                RightTabs.forEach { TabIcon(it, currentDestination, onNavigate) }
            }
        }
    }
}

@Composable
private fun TabIcon(tab: BottomTab, currentDestination: NavDestination?, onNavigate: (Destination) -> Unit) {
    val selected = currentDestination?.hasRoute(tab.destination::class) == true
    IconButton(onClick = { onNavigate(tab.destination) }) {
        Icon(
            imageVector = tab.icon,
            contentDescription = stringResource(tab.labelRes),
            tint = if (selected) Accent else TextSecondary,
        )
    }
}

/**
 * The central log action. Raised and accent-filled so it reads as the primary
 * thing to do on every screen — the product bet is that time-to-log is what
 * decides whether tracking survives past week one.
 */
@Composable
private fun AddEventButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(y = (-10).dp)
            .size(56.dp)
            .shadow(elevation = 12.dp, shape = CircleShape)
            .background(color = Accent, shape = CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.tab_add_event),
            tint = BackgroundPrimary,
            modifier = Modifier.size(28.dp),
        )
    }
}
