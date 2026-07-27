package com.dopamind.app.core.di

import android.content.Context
import com.dopamind.app.core.ai.coach.ChillCoachEngine
import com.dopamind.app.core.ai.craving.CravingPredictionEngine
import com.dopamind.app.core.ai.voice.VoiceCaptureManager
import com.dopamind.app.core.analytics.CorrelationEngine
import com.dopamind.app.core.backup.BackupManager
import com.dopamind.app.core.database.DopaMindDatabase
import com.dopamind.app.core.gamification.BadgeEngine
import com.dopamind.app.core.gamification.GamificationRepository
import com.dopamind.app.core.notifications.NotificationScheduler
import com.dopamind.app.feature.alcohol.data.AlcoholRepository
import com.dopamind.app.feature.cannabis.data.CannabisRepository
import com.dopamind.app.feature.dailyvibe.data.DailyVibeRepository
import com.dopamind.app.feature.dopaminefocus.data.DopamineFocusRepository
import com.dopamind.app.feature.finance.data.FinanceRepository
import com.dopamind.app.feature.libido.data.LibidoRepository
import com.dopamind.app.feature.profile.data.ProfileRepository
import com.dopamind.app.feature.recovery.data.RecoveryRepository
import com.dopamind.app.feature.tobacco.data.TobaccoRepository
import com.dopamind.app.feature.weeklyrecap.domain.AnnualWrappedGenerator
import com.dopamind.app.feature.weeklyrecap.domain.WeeklyRecapGenerator

/**
 * Hand-rolled dependency container (no Hilt/Dagger — keeps the dependency
 * surface small and avoids an annotation-processor toolchain this build
 * couldn't verify against a live Maven resolve). One instance lives on
 * [com.dopamind.app.DopaMindApplication] for the app's lifetime.
 */
class AppContainer(private val appContext: Context) {

    val database: DopaMindDatabase by lazy { DopaMindDatabase.create(appContext) }

    val cannabisRepository by lazy { CannabisRepository(database.cannabisDao()) }
    val tobaccoRepository by lazy { TobaccoRepository(database.tobaccoDao()) }
    val alcoholRepository by lazy { AlcoholRepository(database.alcoholDao()) }
    val libidoRepository by lazy { LibidoRepository(database.libidoDao()) }
    val dopamineFocusRepository by lazy { DopamineFocusRepository(database.dopamineFocusDao()) }
    val recoveryRepository by lazy { RecoveryRepository(database.recoveryDao()) }
    val financeRepository by lazy { FinanceRepository(database.financeDao()) }
    val dailyVibeRepository by lazy { DailyVibeRepository(database.dailyVibeDao()) }
    val gamificationRepository: GamificationRepository by lazy { GamificationRepository(database.gamificationDao()) }
    val profileRepository: ProfileRepository by lazy { ProfileRepository(database.profileDao()) }

    val correlationEngine: CorrelationEngine by lazy {
        CorrelationEngine(
            cannabisRepository = cannabisRepository,
            tobaccoRepository = tobaccoRepository,
            alcoholRepository = alcoholRepository,
            libidoRepository = libidoRepository,
            recoveryRepository = recoveryRepository,
            financeRepository = financeRepository,
            dailyVibeRepository = dailyVibeRepository,
            dopamineFocusRepository = dopamineFocusRepository,
        )
    }

    val weeklyRecapGenerator: WeeklyRecapGenerator by lazy {
        WeeklyRecapGenerator(
            correlationEngine = correlationEngine,
            alcoholRepository = alcoholRepository,
            tobaccoRepository = tobaccoRepository,
            cannabisRepository = cannabisRepository,
            financeRepository = financeRepository,
            recoveryRepository = recoveryRepository,
        )
    }

    val annualWrappedGenerator: AnnualWrappedGenerator by lazy {
        AnnualWrappedGenerator(correlationEngine = correlationEngine, gamificationRepository = gamificationRepository)
    }

    val badgeEngine: BadgeEngine by lazy {
        BadgeEngine(
            gamificationRepository = gamificationRepository,
            dailyVibeRepository = dailyVibeRepository,
            cannabisRepository = cannabisRepository,
            tobaccoRepository = tobaccoRepository,
            alcoholRepository = alcoholRepository,
            libidoRepository = libidoRepository,
            dopamineFocusRepository = dopamineFocusRepository,
            financeRepository = financeRepository,
        )
    }

    val cravingPredictionEngine: CravingPredictionEngine by lazy {
        CravingPredictionEngine(
            cannabisRepository = cannabisRepository,
            tobaccoRepository = tobaccoRepository,
            alcoholRepository = alcoholRepository,
            dopamineFocusRepository = dopamineFocusRepository,
        )
    }

    val chillCoachEngine: ChillCoachEngine by lazy {
        ChillCoachEngine(recoveryRepository = recoveryRepository, dailyVibeRepository = dailyVibeRepository)
    }

    val notificationScheduler: NotificationScheduler by lazy { NotificationScheduler(appContext) }

    val backupManager: BackupManager by lazy { BackupManager(appContext, database) }

    fun createVoiceCaptureManager(): VoiceCaptureManager = VoiceCaptureManager(appContext)
}
