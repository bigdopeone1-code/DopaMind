package com.dopamind.app.core.scoring

/**
 * Every tunable the scoring layer uses, in one place and passed in rather than
 * baked into the algorithms.
 *
 * This exists so the numbers stay reviewable and changeable: product can retune
 * a weight, a test can pin a deterministic config, and a future per-user
 * personalisation pass can hand the engine a derived config instead of the
 * default — none of which is possible when constants are scattered through the
 * calculators.
 *
 * The defaults are product design choices for a behavioural-tracking score.
 * They are not clinical thresholds and carry no medical meaning.
 */
data class ScoringConfig(
    /** Relative importance of each dimension. Renormalised over whatever has data. */
    val dimensionWeights: Map<ScoreDimension, Float> = mapOf(
        ScoreDimension.HABIT_LOAD to 0.30f,
        ScoreDimension.SLEEP to 0.20f,
        ScoreDimension.RECOVERY to 0.18f,
        ScoreDimension.ENERGY to 0.12f,
        ScoreDimension.FOCUS to 0.12f,
        ScoreDimension.STRESS to 0.08f,
    ),

    /** Days of history each evaluation looks at. */
    val windowDays: Int = 7,

    /** Sleep duration treated as a full-marks night for the Sleep dimension. */
    val sleepTargetHours: Float = 8f,
    /** Below this, the Sleep dimension scores 0. */
    val sleepFloorHours: Float = 4f,

    /**
     * Total normalised daily load (summed across categories) treated as a
     * "full load" day — the point where the Habit Load dimension reaches 0.
     * 1.0 means one category at its own daily reference amount.
     */
    val fullLoadThreshold: Float = 2.0f,

    /** More recent days count for more; this is the per-day-older decay factor. */
    val recencyDecay: Float = 0.88f,

    /** Score at or above which a band starts. */
    val bandThresholds: Map<ScoreBand, Int> = mapOf(
        ScoreBand.STRONG to 85,
        ScoreBand.GOOD to 70,
        ScoreBand.STEADY to 50,
        ScoreBand.UNDER_LOAD to 0,
    ),
) {
    fun weightFor(dimension: ScoreDimension): Float = dimensionWeights[dimension] ?: 0f

    fun bandFor(score: Int): ScoreBand =
        bandThresholds.entries
            .sortedByDescending { it.value }
            .firstOrNull { score >= it.value }
            ?.key
            ?: ScoreBand.UNDER_LOAD

    companion object {
        val DEFAULT = ScoringConfig()
    }
}
