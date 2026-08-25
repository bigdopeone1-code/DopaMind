package com.dopamind.app.core.scoring

import com.dopamind.app.core.habit.BodySystem
import com.dopamind.app.core.habit.HabitCategory
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Turns logged behaviour into the per-system readings the Home screen shows on
 * the 3D body.
 *
 * What this is: a weighted roll-up of the user's own logged load, distributed
 * onto systems using the design weights in [HabitCategory.systemLoad], recency-
 * weighted the same way DopaScore is.
 *
 * What this is **not**: a physiological measurement. A reading of "Lungs 95"
 * means "very little of this week's logged load falls in categories associated
 * with the respiratory system", not anything about the user's lungs. The app is
 * a behavioural tracker, not a diagnostic, and no copy built on this output may
 * present it as a health assessment.
 *
 * Pure Kotlin — no Android dependency.
 */
object BodyLoadCalculator {

    fun calculate(input: ScoreInput, config: ScoringConfig = ScoringConfig.DEFAULT): List<BodySystemStatus> =
        BodySystem.entries.map { system -> statusFor(system, input, config) }

    private fun statusFor(system: BodySystem, input: ScoreInput, config: ScoringConfig): BodySystemStatus {
        var weightedLoad = 0.0
        var weightSum = 0.0
        val contributionByCategory = mutableMapOf<HabitCategory, Double>()

        input.days.forEach { day ->
            val daysAgo = (input.todayEpochDay - day.epochDay).coerceAtLeast(0L)
            val recency = config.recencyDecay.toDouble().pow(daysAgo.toDouble())

            var dayLoad = 0.0
            day.loadByCategory.forEach { (category, load) ->
                val systemWeight = category.systemLoad[system] ?: 0f
                if (systemWeight <= 0f) return@forEach
                val contribution = load.toDouble() * systemWeight
                dayLoad += contribution
                contributionByCategory.merge(category, contribution * recency, Double::plus)
            }
            weightedLoad += dayLoad * recency
            weightSum += recency
        }

        val meanLoad = if (weightSum <= 0.0) 0.0 else weightedLoad / weightSum
        val ratio = (meanLoad / config.fullLoadThreshold).coerceIn(0.0, 1.0)
        val value = ((1.0 - ratio) * 100).roundToInt().coerceIn(0, 100)

        return BodySystemStatus(
            system = system,
            value = value,
            topContributor = contributionByCategory.maxByOrNull { it.value }?.key,
        )
    }
}
