package com.dopamind.app.core.scoring

import com.dopamind.app.core.habit.BodySystem
import kotlin.math.pow
import kotlin.math.roundToInt

/** One dimension's raw verdict before weighting. */
data class DimensionResult(val value: Int, val available: Boolean) {
    companion object {
        val UNAVAILABLE = DimensionResult(value = 0, available = false)
        fun of(value: Number) = DimensionResult(value.toDouble().roundToInt().coerceIn(0, 100), available = true)
    }
}

/**
 * A single scoring dimension. Implementations are pure functions of the input —
 * no I/O, no clock, no Android — so each one is independently testable and the
 * set can be swapped or extended without touching the aggregator.
 */
fun interface ScoreDimensionCalculator {
    fun calculate(input: ScoreInput, config: ScoringConfig): DimensionResult
}

/**
 * Composes the configured dimensions into a single DopaScore.
 *
 * Pure Kotlin by design — no Room, no Context, no coroutines. Assembling the
 * [ScoreInput] from repositories is somebody else's job (see
 * `core/scoring/ScoreInputBuilder`), which keeps the actual scoring logic
 * portable and unit-testable, and is what would let this move to a shared KMP
 * module later without touching a line of it.
 *
 * Dimensions with no supporting data are dropped and the remaining weights are
 * renormalised, so the score always reflects what is actually known. How much
 * that is gets reported as [DopaScore.coverage].
 */
class DopaScoreEngine(
    private val calculators: Map<ScoreDimension, ScoreDimensionCalculator> = defaultCalculators(),
    private val config: ScoringConfig = ScoringConfig.DEFAULT,
) {

    fun evaluate(input: ScoreInput): DopaScore {
        val results = ScoreDimension.entries.map { dimension ->
            val calculator = calculators[dimension]
            val result = calculator?.calculate(input, config) ?: DimensionResult.UNAVAILABLE
            DimensionScore(
                dimension = dimension,
                value = result.value,
                available = result.available,
                weight = config.weightFor(dimension),
            )
        }

        val available = results.filter { it.available && it.weight > 0f }
        val totalWeight = available.sumOf { it.weight.toDouble() }.toFloat()
        val configuredWeight = ScoreDimension.entries.sumOf { config.weightFor(it).toDouble() }.toFloat()

        // No dimension had data — report an honest "unknown" rather than a zero
        // the user would read as a bad result.
        if (totalWeight <= 0f) {
            return DopaScore(value = 0, band = ScoreBand.STEADY, dimensions = results, coverage = 0f)
        }

        val weighted = available.sumOf { it.value.toDouble() * it.weight } / totalWeight
        val score = weighted.roundToInt().coerceIn(0, 100)

        return DopaScore(
            value = score,
            band = config.bandFor(score),
            dimensions = results,
            coverage = if (configuredWeight > 0f) (totalWeight / configuredWeight).coerceIn(0f, 1f) else 0f,
        )
    }

    companion object {

        /**
         * Recency-weighted mean: the most recent day counts fully, each older
         * day is discounted by [ScoringConfig.recencyDecay]. Days the selector
         * returns null for are skipped entirely rather than counted as zero.
         */
        private fun weightedMean(
            input: ScoreInput,
            config: ScoringConfig,
            select: (DayAggregate) -> Float?,
        ): Float? {
            var weightedSum = 0.0
            var weightSum = 0.0
            input.days.forEach { day ->
                val value = select(day) ?: return@forEach
                val daysAgo = (input.todayEpochDay - day.epochDay).coerceAtLeast(0L)
                val weight = config.recencyDecay.toDouble().pow(daysAgo.toDouble())
                weightedSum += value * weight
                weightSum += weight
            }
            return if (weightSum <= 0.0) null else (weightedSum / weightSum).toFloat()
        }

        /** Maps a 1..5 self-report onto 0..100. */
        private fun fromFivePointScale(value: Float): Float = ((value - 1f) / 4f).coerceIn(0f, 1f) * 100f

        /**
         * Habit Load — how much of the recent window was spent at or near a
         * full day's load, inverted so lower load scores higher.
         *
         * Known limitation: a user who simply stops logging produces zero load
         * and therefore a high score. The score cannot distinguish "a quiet
         * week" from "an unlogged week", which is why [DopaScore.coverage] is
         * surfaced alongside it and why logging consistency is tracked
         * separately rather than being folded in here as a hidden penalty.
         */
        val HabitLoad = ScoreDimensionCalculator { input, config ->
            if (input.trackedCategories.isEmpty()) return@ScoreDimensionCalculator DimensionResult.UNAVAILABLE
            val meanLoad = weightedMean(input, config) { it.totalLoad } ?: 0f
            val ratio = (meanLoad / config.fullLoadThreshold).coerceIn(0f, 1f)
            DimensionResult.of((1f - ratio) * 100f)
        }

        /** Sleep — average duration mapped from the configured floor to target. */
        val Sleep = ScoreDimensionCalculator { input, config ->
            val meanHours = weightedMean(input, config) { it.sleepHours }
                ?: return@ScoreDimensionCalculator DimensionResult.UNAVAILABLE
            val span = (config.sleepTargetHours - config.sleepFloorHours).takeIf { it > 0f }
                ?: return@ScoreDimensionCalculator DimensionResult.UNAVAILABLE
            val ratio = ((meanHours - config.sleepFloorHours) / span).coerceIn(0f, 1f)
            DimensionResult.of(ratio * 100f)
        }

        /**
         * Recovery — reported sleep quality, plus how many days in the window
         * were genuinely light. Uses whichever of the two signals exist.
         */
        val Recovery = ScoreDimensionCalculator { input, config ->
            val qualityScore = weightedMean(input, config) { day ->
                day.sleepQuality?.toFloat()?.let { fromFivePointScale(it) }
            }
            val lightDayScore = if (input.days.isEmpty() || input.trackedCategories.isEmpty()) {
                null
            } else {
                val lightDays = input.days.count { it.totalLoad < config.fullLoadThreshold * 0.25f }
                (lightDays.toFloat() / input.days.size) * 100f
            }
            val parts = listOfNotNull(qualityScore, lightDayScore)
            if (parts.isEmpty()) DimensionResult.UNAVAILABLE else DimensionResult.of(parts.average())
        }

        /**
         * Focus — load concentrated in the categories weighted towards the
         * brain system, inverted. High attention-fragmenting load scores low.
         */
        val Focus = ScoreDimensionCalculator { input, config ->
            if (input.trackedCategories.isEmpty()) return@ScoreDimensionCalculator DimensionResult.UNAVAILABLE
            val meanBrainLoad = weightedMean(input, config) { day ->
                day.loadByCategory.entries.sumOf { (category, load) ->
                    (load * (category.systemLoad[BodySystem.BRAIN] ?: 0f)).toDouble()
                }.toFloat()
            } ?: 0f
            val ratio = (meanBrainLoad / config.fullLoadThreshold).coerceIn(0f, 1f)
            DimensionResult.of((1f - ratio) * 100f)
        }

        /** Energy — the user's own mood/energy self-report. */
        val Energy = ScoreDimensionCalculator { input, config ->
            val mean = weightedMean(input, config) { day -> day.moodIndex?.toFloat() }
                ?: return@ScoreDimensionCalculator DimensionResult.UNAVAILABLE
            DimensionResult.of(fromFivePointScale(mean))
        }

        /** Stress — self-reported stress, inverted so calmer scores higher. */
        val Stress = ScoreDimensionCalculator { input, config ->
            val mean = weightedMean(input, config) { day -> day.stressIndex?.toFloat() }
                ?: return@ScoreDimensionCalculator DimensionResult.UNAVAILABLE
            DimensionResult.of(100f - fromFivePointScale(mean))
        }

        fun defaultCalculators(): Map<ScoreDimension, ScoreDimensionCalculator> = mapOf(
            ScoreDimension.HABIT_LOAD to HabitLoad,
            ScoreDimension.SLEEP to Sleep,
            ScoreDimension.RECOVERY to Recovery,
            ScoreDimension.FOCUS to Focus,
            ScoreDimension.ENERGY to Energy,
            ScoreDimension.STRESS to Stress,
        )
    }
}
