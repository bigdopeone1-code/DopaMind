package com.dopamind.app.core.ai.hangover

import com.dopamind.app.core.analytics.HangoverRiskCalculator
import com.dopamind.app.core.analytics.model.HangoverRiskResult
import com.dopamind.app.feature.alcohol.domain.BacCalculator
import com.dopamind.app.feature.alcohol.domain.BacResult
import com.dopamind.app.feature.alcohol.domain.BiologicalSex
import com.dopamind.app.feature.alcohol.domain.DrinkInput

/**
 * Live, mid-session wrapper around [BacCalculator] + [HangoverRiskCalculator]:
 * recompute risk on every new drink logged tonight, and suggest a next
 * hydration checkpoint before it gets bad rather than after.
 */
data class LiveHangoverAssessment(
    val bac: BacResult,
    val risk: HangoverRiskResult,
    val nextWaterReminderInMinutes: Int,
)

object PredictiveHangoverEngine {

    fun assessLive(
        drinksTonight: List<DrinkInput>,
        bodyWeightKg: Double,
        sex: BiologicalSex,
        glassesOfWaterSoFar: Int,
        ateFoodTonight: Boolean,
        plannedSleepHours: Float,
        averageDrinksLastMonth: Double,
        nowEpochMillis: Long,
    ): LiveHangoverAssessment {
        val bac = BacCalculator.estimate(drinksTonight, bodyWeightKg, sex, nowEpochMillis)
        val risk = HangoverRiskCalculator.estimate(
            bac = bac,
            glassesOfWater = glassesOfWaterSoFar,
            ateFoodTonight = ateFoodTonight,
            plannedSleepHours = plannedSleepHours,
            averageDrinksLastMonth = averageDrinksLastMonth,
        )

        // The faster BAC is rising, the sooner the next glass of water should land.
        val drinksInLastHour = drinksTonight.count { nowEpochMillis - it.timestampEpochMillis <= 3_600_000L }
        val nextReminder = when {
            drinksInLastHour >= 3 -> 15
            drinksInLastHour == 2 -> 25
            drinksInLastHour == 1 -> 40
            else -> 60
        }

        return LiveHangoverAssessment(bac = bac, risk = risk, nextWaterReminderInMinutes = nextReminder)
    }
}
