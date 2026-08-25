package com.dopamind.app.core.scoring

import com.dopamind.app.core.habit.BodySystem
import com.dopamind.app.core.habit.HabitCategory

/**
 * The six dimensions DopaScore is composed of. Each is scored independently on
 * 0..100 and then weighted; adding a seventh dimension means adding a constant
 * here, a [ScoreDimensionCalculator] for it, and a weight in [ScoringConfig] —
 * no other file changes.
 */
enum class ScoreDimension { RECOVERY, SLEEP, FOCUS, ENERGY, STRESS, HABIT_LOAD }

/**
 * Descriptive bands, never verdicts. The product's language rule is that the
 * app describes what the data shows and never grades the person, so these map
 * to neutral copy ("steady", "under load") rather than praise or blame.
 */
enum class ScoreBand { STRONG, GOOD, STEADY, UNDER_LOAD }

/**
 * One dimension's contribution.
 *
 * [available] is the important field: a dimension with no underlying data is
 * *excluded* and the remaining weights are renormalised, rather than being
 * scored 0. Someone who has never logged sleep should not be shown a depressed
 * DopaScore because of it — the score reports on what it actually knows.
 */
data class DimensionScore(
    val dimension: ScoreDimension,
    val value: Int, // 0..100
    val available: Boolean,
    val weight: Float,
)

data class DopaScore(
    val value: Int, // 0..100
    val band: ScoreBand,
    val dimensions: List<DimensionScore>,
    /** 0..1 — share of the configured weight that had real data behind it. */
    val coverage: Float,
) {
    val availableDimensions: List<DimensionScore> get() = dimensions.filter { it.available }

    /** The dimension currently pulling the score down the most, if any. */
    val weakestDimension: DimensionScore? get() = availableDimensions.minByOrNull { it.value }

    /** The dimension currently holding the score up the most, if any. */
    val strongestDimension: DimensionScore? get() = availableDimensions.maxByOrNull { it.value }
}

/**
 * A body system's current exposure reading.
 *
 * [value] is an *exposure index*, not a health measurement: 100 means little
 * or none of the recent logged load maps onto behaviours associated with this
 * system, 0 means a lot of it does. It is derived purely from what the user
 * logged and the design weights in [HabitCategory.systemLoad]. It is not a
 * physiological reading and no copy anywhere may present it as one.
 */
data class BodySystemStatus(
    val system: BodySystem,
    val value: Int, // 0..100
    val topContributor: HabitCategory?,
)

/** Everything the scoring layer needs for one evaluation, assembled by the caller. */
data class ScoreInput(
    val todayEpochDay: Long,
    /** Per-day, oldest first, one entry per day in the scoring window. */
    val days: List<DayAggregate>,
    val trackedCategories: List<HabitCategory>,
)

/**
 * One calendar day reduced to the signals the scoring layer consumes. Keeping
 * this separate from the raw event rows is what lets the engine stay pure
 * Kotlin — no Room, no Android, trivially unit-testable.
 */
data class DayAggregate(
    val epochDay: Long,
    /** Normalised 0..1 load per category, already divided by its daily reference. */
    val loadByCategory: Map<HabitCategory, Float>,
    val sleepHours: Float?,
    val sleepQuality: Int?, // 1..5
    val moodIndex: Int?, // 1..5
    val stressIndex: Int?, // 1..5, higher = more stress reported
) {
    val totalLoad: Float get() = loadByCategory.values.sum()
    val hasAnyEvent: Boolean get() = loadByCategory.isNotEmpty()
}
