package com.dopamind.app.core.analytics

import com.dopamind.app.core.analytics.model.HangoverRiskLevel
import com.dopamind.app.core.analytics.model.HangoverRiskResult
import com.dopamind.app.core.analytics.model.HangoverTip
import com.dopamind.app.feature.alcohol.domain.BacResult

/**
 * Pure Kotlin — no Android dependency.
 *
 * Heuristic, informational-only estimate of next-day hangover risk, used
 * both retrospectively (Alcohol module) and live, mid-session, by the
 * AI Predictive Hangover engine.
 */
object HangoverRiskCalculator {

    fun estimate(
        bac: BacResult,
        glassesOfWater: Int,
        ateFoodTonight: Boolean,
        plannedSleepHours: Float,
        averageDrinksLastMonth: Double,
    ): HangoverRiskResult {
        var score = (bac.bacGramsPerLiter * 55).coerceAtMost(70.0)

        if (!ateFoodTonight) score += 12
        if (glassesOfWater < 2) score += 10 else score -= (glassesOfWater * 2).coerceAtMost(15)
        if (plannedSleepHours < 6f) score += 10

        // Built-up tolerance slightly softens the curve; low tolerance sharpens it.
        val toleranceAdjustment = when {
            averageDrinksLastMonth > bac.gramsAlcoholTotal / 14.0 -> -5.0
            averageDrinksLastMonth < 1.0 -> 8.0
            else -> 0.0
        }
        score += toleranceAdjustment

        val finalScore = score.coerceIn(0.0, 100.0).toInt()

        val level = when {
            finalScore < 25 -> HangoverRiskLevel.LOW
            finalScore < 50 -> HangoverRiskLevel.MODERATE
            finalScore < 75 -> HangoverRiskLevel.HIGH
            else -> HangoverRiskLevel.SEVERE
        }

        val tips = buildList {
            if (glassesOfWater < 3) add(HangoverTip.DRINK_WATER)
            if (!ateFoodTonight) add(HangoverTip.EAT_BEFORE_SLEEP)
            if (finalScore >= 50) add(HangoverTip.ELECTROLYTES)
            if (plannedSleepHours < 7f) add(HangoverTip.SLEEP_EARLY)
            if (bac.isStillRising) add(HangoverTip.SLOW_DOWN)
            if (finalScore >= 75) add(HangoverTip.STOP_DRINKING)
        }

        return HangoverRiskResult(riskScore = finalScore, level = level, tips = tips)
    }
}
