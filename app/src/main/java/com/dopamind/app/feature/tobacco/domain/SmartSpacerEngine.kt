package com.dopamind.app.feature.tobacco.domain

import kotlin.math.max
import kotlin.math.roundToLong

/** Pure Kotlin — no Android dependency. */
data class SpacerSuggestion(
    val averageIntervalMinutesToday: Int,
    val nextSuggestedEpochMillis: Long,
    val minutesUntilNextSuggested: Long,
    val todayCount: Int,
    val dailyTarget: Int,
    val onTrackForTarget: Boolean,
)

/**
 * Nudges the gap between puffs/sticks slightly wider than the user's own
 * recent average, so the daily count trends down over time instead of
 * staying flat — without ever blocking a log.
 */
object SmartSpacerEngine {

    private const val STRETCH_FACTOR = 1.15 // widen the next gap by 15% over the recent average
    private const val MIN_GAP_MINUTES = 20L

    fun suggest(
        todayTimestampsEpochMillis: List<Long>,
        dailyTarget: Int,
        nowEpochMillis: Long,
    ): SpacerSuggestion {
        val sorted = todayTimestampsEpochMillis.sorted()
        val averageIntervalMinutes = if (sorted.size >= 2) {
            val gaps = sorted.zipWithNext { a, b -> b - a }
            (gaps.average() / 60_000.0).roundToLong().toInt()
        } else {
            0
        }

        val lastTimestamp = sorted.lastOrNull() ?: nowEpochMillis
        val gapMinutes = max(MIN_GAP_MINUTES, (averageIntervalMinutes * STRETCH_FACTOR).roundToLong())
        val nextSuggested = lastTimestamp + gapMinutes * 60_000L

        return SpacerSuggestion(
            averageIntervalMinutesToday = averageIntervalMinutes,
            nextSuggestedEpochMillis = nextSuggested,
            minutesUntilNextSuggested = max(0L, (nextSuggested - nowEpochMillis) / 60_000L),
            todayCount = sorted.size,
            dailyTarget = dailyTarget,
            onTrackForTarget = sorted.size <= dailyTarget,
        )
    }
}
