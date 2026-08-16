package com.dopamind.app.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose destinations. Every route in the app is one
 * of these — no string-based route/argument passing anywhere, so a typo in
 * a module can no longer break routing in another one at runtime.
 */
sealed interface Destination {

    /** Decides, once the profile finishes loading, whether to route to Onboarding or Dashboard. */
    @Serializable
    data object Splash : Destination

    @Serializable
    data object Onboarding : Destination

    @Serializable
    data object Profile : Destination

    @Serializable
    data object BadgeGallery : Destination

    @Serializable
    data class ModuleHistory(val module: HistoryModule) : Destination

    @Serializable
    data object Dashboard : Destination

    @Serializable
    data object DailyVibeCheckIn : Destination

    @Serializable
    data object WeeklyRecap : Destination

    @Serializable
    data object AnnualWrapped : Destination

    @Serializable
    data object CannabisHome : Destination

    @Serializable
    data object TobaccoHome : Destination

    @Serializable
    data object AlcoholHome : Destination

    @Serializable
    data object LibidoHome : Destination

    @Serializable
    data object DopamineFocusHome : Destination

    @Serializable
    data object RecoveryHome : Destination

    @Serializable
    data object FinanceHome : Destination

    @Serializable
    data object NutritionHome : Destination

    /** The SOS / bad-trip breathing & rassurance flow, reachable from Recovery or the dashboard. */
    @Serializable
    data object ChillCoachSos : Destination

    /** Hands-free logging entry point, reachable from the dashboard. */
    @Serializable
    data object VoiceLog : Destination

    /** Camera-based drink label / food scanner, reachable from Alcohol and Recovery. */
    @Serializable
    data class VisionScanner(val target: ScanTarget) : Destination
}

@Serializable
enum class ScanTarget { DRINK_LABEL, FOOD_PLATE }

@Serializable
enum class HistoryModule { CANNABIS, TOBACCO, ALCOHOL, LIBIDO, SLEEP }
