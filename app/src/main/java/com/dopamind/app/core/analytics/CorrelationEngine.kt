package com.dopamind.app.core.analytics

import com.dopamind.app.core.analytics.model.DailyMetrics
import com.dopamind.app.core.analytics.model.DopamineDebtResult
import com.dopamind.app.core.analytics.model.SleepCorrelation
import com.dopamind.app.feature.alcohol.data.AlcoholRepository
import com.dopamind.app.feature.alcohol.domain.BacCalculator
import com.dopamind.app.feature.cannabis.data.CannabisRepository
import com.dopamind.app.feature.dailyvibe.data.DailyVibeRepository
import com.dopamind.app.feature.dopaminefocus.data.DopamineFocusRepository
import com.dopamind.app.feature.finance.data.FinanceRepository
import com.dopamind.app.feature.libido.data.LibidoRepository
import com.dopamind.app.feature.recovery.data.RecoveryRepository
import com.dopamind.app.feature.tobacco.data.TobaccoRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * core/analytics' central aggregation engine — the one place in the app that
 * reads across every module's repository to build the day-by-day picture
 * consumed by the Dopamine Debt score, sleep correlations, Weekly Recap, and
 * gamification.
 */
class CorrelationEngine(
    private val cannabisRepository: CannabisRepository,
    private val tobaccoRepository: TobaccoRepository,
    private val alcoholRepository: AlcoholRepository,
    private val libidoRepository: LibidoRepository,
    private val recoveryRepository: RecoveryRepository,
    private val financeRepository: FinanceRepository,
    private val dailyVibeRepository: DailyVibeRepository,
    private val dopamineFocusRepository: DopamineFocusRepository,
) {

    fun todayEpochDay(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    suspend fun aggregateDailyMetrics(daysBack: Int, endEpochDay: Long = todayEpochDay()): List<DailyMetrics> {
        val startEpochDay = endEpochDay - daysBack + 1
        val sinceMillis = startEpochDay * MILLIS_PER_DAY

        val cannabisLogs = cannabisRepository.observeLogsSince(sinceMillis).first()
        val tobaccoLogs = tobaccoRepository.observeLogsSince(sinceMillis).first()
        val drinkLogs = alcoholRepository.observeLogsSince(sinceMillis).first()
        val libidoLogs = libidoRepository.observeLogs().first().filter { it.timestampEpochMillis >= sinceMillis }
        val sleepLogs = recoveryRepository.observeSleepLogsSince(startEpochDay).first()
        val vibeLogs = dailyVibeRepository.observeSince(startEpochDay).first()
        val spendLogs = financeRepository.observeSpendsSince(sinceMillis).first()

        return (startEpochDay..endEpochDay).map { day ->
            val dayStart = day * MILLIS_PER_DAY
            val dayEnd = dayStart + MILLIS_PER_DAY
            val dayDrinks = drinkLogs.filter { it.timestampEpochMillis in dayStart until dayEnd }
            val sleepForDay = sleepLogs.find { it.dateEpochDay == day }

            DailyMetrics(
                epochDay = day,
                cannabisUses = cannabisLogs.count { it.timestampEpochMillis in dayStart until dayEnd },
                tobaccoUses = tobaccoLogs.count { it.timestampEpochMillis in dayStart until dayEnd },
                alcoholStandardDrinks = dayDrinks.size,
                alcoholGramsPure = dayDrinks.sumOf { BacCalculator.gramsOfAlcohol(it.volumeMl, it.abvPercent) },
                libidoEvents = libidoLogs.count { it.timestampEpochMillis in dayStart until dayEnd },
                sleepHours = sleepForDay?.hours,
                sleepQuality = sleepForDay?.quality,
                moodEnergyIndex = vibeLogs.find { it.dateEpochDay == day }?.moodEnergyIndex,
                spendEuros = spendLogs.filter { it.timestampEpochMillis in dayStart until dayEnd }.sumOf { it.amountEuros.toDouble() }.toFloat(),
            )
        }
    }

    suspend fun computeDopamineDebt(): DopamineDebtResult {
        val days = aggregateDailyMetrics(daysBack = 14)
        val recentWindowStart = todayEpochDay() - 6
        val detoxSessions = dopamineFocusRepository.observeDetoxSessions().first()
            .count { it.completed && it.endEpochMillis != null && it.endEpochMillis!! / MILLIS_PER_DAY >= recentWindowStart }
        return DopamineDebtCalculator.compute(days, detoxSessions)
    }

    suspend fun computeSleepCorrelations(daysBack: Int = 30): List<SleepCorrelation> =
        SleepCorrelationAnalyzer.analyze(aggregateDailyMetrics(daysBack))
}
