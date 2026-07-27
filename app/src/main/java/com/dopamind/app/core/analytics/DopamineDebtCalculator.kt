package com.dopamind.app.core.analytics

import com.dopamind.app.core.analytics.model.DailyMetrics
import com.dopamind.app.core.analytics.model.DebtContributor
import com.dopamind.app.core.analytics.model.DopamineDebtResult
import com.dopamind.app.core.analytics.model.Trend
import kotlin.math.pow

/**
 * Pure Kotlin — no Android dependency.
 *
 * A playful, non-clinical "load score": recent high-stimulation activity
 * (cannabis/tobacco/alcohol/libido) accumulates debt, recency-weighted;
 * good sleep and completed detox sessions pay it down. This is entertainment
 * framing inspired by pop "dopamine fasting" ideas, not a medical metric.
 */
object DopamineDebtCalculator {

    private const val WINDOW_DAYS = 7
    private const val DECAY_PER_DAY = 0.85
    private const val SCALE_TO_100 = 2.2

    private const val WEIGHT_CANNABIS = 3.0
    private const val WEIGHT_TOBACCO = 1.5
    private const val WEIGHT_ALCOHOL = 4.0
    private const val WEIGHT_LIBIDO = 1.5
    private const val WEIGHT_POOR_SLEEP = 2.0
    private const val POOR_SLEEP_THRESHOLD_HOURS = 6f

    fun compute(last14Days: List<DailyMetrics>, detoxSessionsCompletedInWindow: Int): DopamineDebtResult {
        val sorted = last14Days.sortedBy { it.epochDay }
        val recentWindow = sorted.takeLast(WINDOW_DAYS)
        val previousWindow = sorted.dropLast(WINDOW_DAYS).takeLast(WINDOW_DAYS)

        val recentScore = weightedScore(recentWindow)
        val previousScore = weightedScore(previousWindow)

        val debtPaydown = (detoxSessionsCompletedInWindow * 5.0).coerceAtMost(30.0)
        val normalized = ((recentScore * SCALE_TO_100) - debtPaydown).coerceIn(0.0, 100.0)

        val trend = when {
            previousWindow.isEmpty() -> Trend.STABLE
            recentScore > previousScore * 1.1 -> Trend.RISING
            recentScore < previousScore * 0.9 -> Trend.FALLING
            else -> Trend.STABLE
        }

        val contributions = mapOf(
            DebtContributor.CANNABIS to recentWindow.sumOf { it.cannabisUses } * WEIGHT_CANNABIS,
            DebtContributor.TOBACCO to recentWindow.sumOf { it.tobaccoUses } * WEIGHT_TOBACCO,
            DebtContributor.ALCOHOL to recentWindow.sumOf { it.alcoholStandardDrinks } * WEIGHT_ALCOHOL,
            DebtContributor.LIBIDO to recentWindow.sumOf { it.libidoEvents } * WEIGHT_LIBIDO,
            DebtContributor.POOR_SLEEP to recentWindow.count { (it.sleepHours ?: 8f) < POOR_SLEEP_THRESHOLD_HOURS } * WEIGHT_POOR_SLEEP,
        )
        val topContributors = contributions.entries
            .sortedByDescending { it.value }
            .filter { it.value > 0.0 }
            .take(3)
            .map { it.key }

        return DopamineDebtResult(
            score = normalized.toInt(),
            trend = trend,
            topContributors = topContributors,
        )
    }

    private fun weightedScore(days: List<DailyMetrics>): Double {
        if (days.isEmpty()) return 0.0
        val mostRecentDay = days.maxOf { it.epochDay }
        return days.sumOf { day ->
            val daysAgo = (mostRecentDay - day.epochDay).toInt()
            val decay = DECAY_PER_DAY.pow(daysAgo)
            val poorSleepPoints = if ((day.sleepHours ?: 8f) < POOR_SLEEP_THRESHOLD_HOURS) WEIGHT_POOR_SLEEP else 0.0
            val dayScore = day.cannabisUses * WEIGHT_CANNABIS +
                day.tobaccoUses * WEIGHT_TOBACCO +
                day.alcoholStandardDrinks * WEIGHT_ALCOHOL +
                day.libidoEvents * WEIGHT_LIBIDO +
                poorSleepPoints
            dayScore * decay
        }
    }
}
