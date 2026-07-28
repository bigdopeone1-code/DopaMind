package com.dopamind.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dopamind.app.core.di.LocalAppContainer
import com.dopamind.app.core.navigation.BottomBarDestinationClasses
import com.dopamind.app.core.navigation.DopaMindBottomBar
import com.dopamind.app.core.navigation.DopaMindNavHost
import com.dopamind.app.core.theme.BackgroundPrimary
import com.dopamind.app.core.theme.DopaMindTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as DopaMindApplication).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                DopaMindTheme {
                    val profile by container.profileRepository.observeProfile().collectAsStateWithLifecycle(initialValue = null)
                    val view = LocalView.current
                    LaunchedEffect(profile?.immersiveModeEnabled) {
                        val controller = WindowCompat.getInsetsController(window, view)
                        if (profile?.immersiveModeEnabled == true) {
                            controller.hide(WindowInsetsCompat.Type.systemBars())
                            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        } else {
                            controller.show(WindowInsetsCompat.Type.systemBars())
                        }
                    }
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = backStackEntry?.destination
                    val showBottomBar = currentDestination?.let { dest ->
                        BottomBarDestinationClasses.any { dest.hasRoute(it) }
                    } == true

                    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundPrimary) {
                        Scaffold(
                            containerColor = Color.Transparent,
                            bottomBar = {
                                if (showBottomBar) {
                                    DopaMindBottomBar(currentDestination = currentDestination, onNavigate = { destination ->
                                        navController.navigate(destination) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    })
                                }
                            },
                        ) { innerPadding ->
                            DopaMindNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }
}
