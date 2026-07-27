package com.dopamind.app.feature.weeklyrecap.domain

import com.dopamind.app.core.analytics.CorrelationEngine
import com.dopamind.app.core.gamification.GamificationRepository
import kotlinx.coroutines.flow.first

private const val YEAR_DAYS = 365

enum class MostActiveModule { CANNABIS, TOBACCO, ALCOHOL, LIBIDO, NONE }

data class AnnualWrapped(
    val totalSpendEuros: Float,
    val mostActiveModule: MostActiveModule,
    val badgesUnlocked: Int,
    val bestSmokeFreeStreakDays: Int,
    val averageSleepHours: Float?,
)

/** The once-a-year, bigger sibling of [WeeklyRecapGenerator]'s weekly cards. */
class AnnualWrappedGenerator(
    private val correlationEngine: CorrelationEngine,
    private val gamificationRepository: GamificationRepository,
) {
    suspend fun generate(): AnnualWrapped {
        val days = correlationEngine.aggregateDailyMetrics(YEAR_DAYS)

        val totalSpend = days.sumOf { it.spendEuros.toDouble() }.toFloat()

        val totals = mapOf(
            MostActiveModule.CANNABIS to days.sumOf { it.cannabisUses },
            MostActiveModule.TOBACCO to days.sumOf { it.tobaccoUses },
            MostActiveModule.ALCOHOL to days.sumOf { it.alcoholStandardDrinks },
            MostActiveModule.LIBIDO to days.sumOf { it.libidoEvents },
        )
        val mostActive = totals.entries.maxByOrNull { it.value }
            ?.takeIf { it.value > 0 }?.key ?: MostActiveModule.NONE

        var bestStreak = 0
        var currentStreak = 0
        for (day in days.sortedBy { it.epochDay }) {
            if (day.tobaccoUses == 0) {
                currentStreak++
                bestStreak = maxOf(bestStreak, currentStreak)
            } else {
                currentStreak = 0
            }
        }

        val sleepValues = days.mapNotNull { it.sleepHours }
        val averageSleep = if (sleepValues.isNotEmpty()) sleepValues.average().toFloat() else null

        return AnnualWrapped(
            totalSpendEuros = totalSpend,
            mostActiveModule = mostActive,
            badgesUnlocked = gamificationRepository.unlockedBadgeIds().size,
            bestSmokeFreeStreakDays = bestStreak,
            averageSleepHours = averageSleep,
        )
    }
}
