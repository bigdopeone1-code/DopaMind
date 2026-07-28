package com.dopamind.app.feature.weeklyrecap.domain

import com.dopamind.app.core.analytics.CorrelationEngine
import com.dopamind.app.feature.alcohol.data.AlcoholRepository
import com.dopamind.app.feature.cannabis.data.CannabisRepository
import com.dopamind.app.feature.finance.data.FinanceRepository
import com.dopamind.app.feature.finance.data.SpendCategory
import com.dopamind.app.feature.recovery.data.RecoveryRepository
import com.dopamind.app.feature.tobacco.data.TobaccoRepository
import kotlinx.coroutines.flow.first

private const val WEEK_DAYS = 7
private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Builds the "Spotify Wrapped"-style set of weekly stat cards from
 * core/analytics' aggregated data plus a couple of category-specific
 * lookups (spend by category, most-logged drink type) for flavor.
 */
class WeeklyRecapGenerator(
    private val correlationEngine: CorrelationEngine,
    private val alcoholRepository: AlcoholRepository,
    private val tobaccoRepository: TobaccoRepository,
    private val cannabisRepository: CannabisRepository,
    private val financeRepository: FinanceRepository,
    private val recoveryRepository: RecoveryRepository,
) {
    suspend fun generate(): List<RecapCard> {
        val today = correlationEngine.todayEpochDay()
        val weekStart = today - WEEK_DAYS + 1
        val weekStartMillis = weekStart * MILLIS_PER_DAY

        val days = correlationEngine.aggregateDailyMetrics(WEEK_DAYS)
        if (days.all { it.cannabisUses == 0 && it.tobaccoUses == 0 && it.alcoholStandardDrinks == 0 && it.libidoEvents == 0 && it.sleepHours == null }) {
            return listOf(RecapCard(RecapCardType.NO_DATA))
        }

        val totalSpend = days.sumOf { it.spendEuros.toDouble() }.toFloat()

        val alcoholSpend = financeRepository.observeSpendsForCategory(SpendCategory.ALCOHOL).first()
            .filter { it.timestampEpochMillis >= weekStartMillis }
            .sumOf { it.amountEuros.toDouble() }.toFloat()

        val mostLoggedDrinkType = alcoholRepository.observeLogsSince(weekStartMillis).first()
            .groupingBy { it.drinkType }.eachCount().maxByOrNull { it.value }?.key

        val smokeFreeDays = days.count { it.tobaccoUses == 0 }
        val cannabisSessions = days.sumOf { it.cannabisUses }
        val sleepValues = days.mapNotNull { it.sleepHours }
        val sleepAverage = if (sleepValues.isNotEmpty()) sleepValues.average().toFloat() else null
        val moodValues = days.mapNotNull { it.moodEnergyIndex }
        val moodAverage = if (moodValues.isNotEmpty()) moodValues.average().toFloat() else null

        val debt = correlationEngine.computeDopamineDebt()

        return buildList {
            if (totalSpend > 0f) add(RecapCard(RecapCardType.TOTAL_SPEND, primaryValue = totalSpend))
            if (alcoholSpend > 0f) add(RecapCard(RecapCardType.ALCOHOL_SPEND, primaryValue = alcoholSpend, secondaryLabel = mostLoggedDrinkType))
            if (smokeFreeDays > 0) add(RecapCard(RecapCardType.SMOKE_FREE_DAYS, primaryValue = smokeFreeDays.toFloat()))
            if (cannabisSessions > 0) add(RecapCard(RecapCardType.CANNABIS_SESSIONS, primaryValue = cannabisSessions.toFloat()))
            sleepAverage?.let { add(RecapCard(RecapCardType.SLEEP_AVERAGE, primaryValue = it)) }
            add(RecapCard(RecapCardType.DOPAMINE_DEBT_TREND, primaryValue = debt.score.toFloat(), trend = debt.trend))
            moodAverage?.let { add(RecapCard(RecapCardType.MOOD_AVERAGE, primaryValue = it)) }
        }
    }
}
