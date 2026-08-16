package com.dopamind.app.core.analytics

import com.dopamind.app.core.analytics.model.DailyMetrics
import com.dopamind.app.core.analytics.model.DopamineDebtResult

enum class ModuleScoreStatus { OPTIMAL, GOOD, FAIR, LOW }

data class ModuleScore(val value: Int, val status: ModuleScoreStatus)

/**
 * Pure Kotlin — no Android dependency.
 *
 * A 0-100 score per module for the Dashboard's module cards. Cannabis/
 * Tobacco/Alcohol/Libido use a "moderation score" (higher recent use ->
 * lower score), consistent with DopamineDebtCalculator's own view of these
 * as load contributors. Focus reuses the real Dopamine Debt score (inverted).
 * Recovery uses a real sleep-hygiene proxy (average hours vs. 8h).
 */
object ModuleScoreCalculator {

    private fun statusFor(score: Int) = when {
        score >= 75 -> ModuleScoreStatus.OPTIMAL
        score >= 50 -> ModuleScoreStatus.GOOD
        score >= 25 -> ModuleScoreStatus.FAIR
        else -> ModuleScoreStatus.LOW
    }

    private fun moderationScore(usesLast7Days: Int, weeklyThreshold: Int): ModuleScore {
        val ratio = (usesLast7Days.toFloat() / weeklyThreshold).coerceIn(0f, 1f)
        val value = (100 - ratio * 100).toInt()
        return ModuleScore(value, statusFor(value))
    }

    fun cannabis(last7Days: List<DailyMetrics>) = moderationScore(last7Days.sumOf { it.cannabisUses }, weeklyThreshold = 7)
    fun tobacco(last7Days: List<DailyMetrics>) = moderationScore(last7Days.sumOf { it.tobaccoUses }, weeklyThreshold = 35)
    fun alcohol(last7Days: List<DailyMetrics>) = moderationScore(last7Days.sumOf { it.alcoholStandardDrinks }, weeklyThreshold = 14)
    fun libido(last7Days: List<DailyMetrics>) = moderationScore(last7Days.sumOf { it.libidoEvents }, weeklyThreshold = 14)

    fun focus(debt: DopamineDebtResult): ModuleScore {
        val value = (100 - debt.score).coerceIn(0, 100)
        return ModuleScore(value, statusFor(value))
    }

    fun recovery(last7Days: List<DailyMetrics>): ModuleScore {
        val sleepValues = last7Days.mapNotNull { it.sleepHours }
        if (sleepValues.isEmpty()) return ModuleScore(0, ModuleScoreStatus.LOW)
        val avgHours = sleepValues.average()
        val value = ((avgHours / 8.0) * 100).coerceIn(0.0, 100.0).toInt()
        return ModuleScore(value, statusFor(value))
    }

    /** A lightweight, real-data-derived per-day proxy for the Dashboard sparkline — not
     * the authoritative Dopamine Debt calculation (DopamineDebtCalculator owns that),
     * just a same-shaped daily trend built from the same raw signals. */
    fun dailyScoreProxy(day: DailyMetrics): Float {
        val load = day.cannabisUses * 3f + day.tobaccoUses * 1.5f + day.alcoholStandardDrinks * 4f + day.libidoEvents * 1.5f
        return (100f - load * 2.2f).coerceIn(0f, 100f)
    }
}
