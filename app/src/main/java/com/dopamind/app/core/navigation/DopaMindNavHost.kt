package com.dopamind.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.dopamind.app.feature.alcohol.ui.AlcoholScreen
import com.dopamind.app.feature.badges.ui.BadgeGalleryScreen
import com.dopamind.app.feature.cannabis.ui.CannabisScreen
import com.dopamind.app.feature.dailyvibe.ui.DailyVibeCheckInScreen
import com.dopamind.app.feature.dashboard.ui.DashboardScreen
import com.dopamind.app.feature.dopaminefocus.ui.DopamineFocusScreen
import com.dopamind.app.feature.finance.ui.FinanceScreen
import com.dopamind.app.feature.history.ui.HistoryHubScreen
import com.dopamind.app.feature.history.ui.ModuleHistoryScreen
import com.dopamind.app.feature.libido.ui.LibidoScreen
import com.dopamind.app.feature.profile.ui.OnboardingScreen
import com.dopamind.app.feature.profile.ui.ProfileScreen
import com.dopamind.app.feature.profile.ui.SplashScreen
import com.dopamind.app.feature.recovery.ui.ChillCoachSosScreen
import com.dopamind.app.feature.recovery.ui.RecoveryScreen
import com.dopamind.app.feature.scanner.ui.VisionScannerScreen
import com.dopamind.app.feature.tobacco.ui.TobaccoScreen
import com.dopamind.app.feature.voicelog.ui.VoiceLogScreen
import com.dopamind.app.feature.weeklyrecap.ui.AnnualWrappedScreen
import com.dopamind.app.feature.weeklyrecap.ui.WeeklyRecapScreen

@Composable
fun DopaMindNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = Destination.Splash, modifier = modifier) {

        composable<Destination.Splash> {
            SplashScreen(
                onNeedsOnboarding = {
                    navController.navigate(Destination.Onboarding) {
                        popUpTo(Destination.Splash) { inclusive = true }
                    }
                },
                onReady = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Splash) { inclusive = true }
                    }
                },
            )
        }

        composable<Destination.Onboarding> {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Onboarding) { inclusive = true }
                    }
                },
            )
        }

        composable<Destination.Profile> {
            ProfileScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.BadgeGallery> {
            BadgeGalleryScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.ModuleHistory> { backStackEntry ->
            val route: Destination.ModuleHistory = backStackEntry.toRoute()
            ModuleHistoryScreen(module = route.module, onBack = { navController.popBackStack() })
        }

        composable<Destination.HistoryHub> {
            HistoryHubScreen(
                onBack = { navController.popBackStack() },
                onSelectModule = { module -> navController.navigate(Destination.ModuleHistory(module)) },
            )
        }

        composable<Destination.Dashboard> {
            DashboardScreen(onNavigate = { destination -> navController.navigate(destination) })
        }

        composable<Destination.DailyVibeCheckIn> {
            DailyVibeCheckInScreen(
                onFinish = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable<Destination.WeeklyRecap> {
            WeeklyRecapScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.AnnualWrapped> {
            AnnualWrappedScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.CannabisHome> {
            CannabisScreen(
                onBack = { navController.popBackStack() },
                onViewHistory = { navController.navigate(Destination.ModuleHistory(HistoryModule.CANNABIS)) },
            )
        }

        composable<Destination.TobaccoHome> {
            TobaccoScreen(
                onBack = { navController.popBackStack() },
                onViewHistory = { navController.navigate(Destination.ModuleHistory(HistoryModule.TOBACCO)) },
            )
        }

        composable<Destination.AlcoholHome> {
            AlcoholScreen(
                onBack = { navController.popBackStack() },
                onScanLabel = { navController.navigate(Destination.VisionScanner(ScanTarget.DRINK_LABEL)) },
                onViewHistory = { navController.navigate(Destination.ModuleHistory(HistoryModule.ALCOHOL)) },
            )
        }

        composable<Destination.LibidoHome> {
            LibidoScreen(
                onBack = { navController.popBackStack() },
                onViewHistory = { navController.navigate(Destination.ModuleHistory(HistoryModule.LIBIDO)) },
            )
        }

        composable<Destination.DopamineFocusHome> {
            DopamineFocusScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.RecoveryHome> {
            RecoveryScreen(
                onBack = { navController.popBackStack() },
                onSos = { navController.navigate(Destination.ChillCoachSos) },
                onScanFood = { navController.navigate(Destination.VisionScanner(ScanTarget.FOOD_PLATE)) },
                onViewSleepHistory = { navController.navigate(Destination.ModuleHistory(HistoryModule.SLEEP)) },
            )
        }

        composable<Destination.FinanceHome> {
            FinanceScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.ChillCoachSos> {
            ChillCoachSosScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.VoiceLog> {
            VoiceLogScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.VisionScanner> { backStackEntry ->
            val route: Destination.VisionScanner = backStackEntry.toRoute()
            VisionScannerScreen(target = route.target, onDone = { navController.popBackStack() })
        }
    }
}
