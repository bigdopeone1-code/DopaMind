package com.dopamind.app.core.ai.craving

import com.dopamind.app.feature.alcohol.data.AlcoholRepository
import com.dopamind.app.feature.cannabis.data.CannabisRepository
import com.dopamind.app.feature.dopaminefocus.data.DopamineFocusRepository
import com.dopamind.app.feature.tobacco.data.TobaccoRepository
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

enum class TrackedModule { CANNABIS, TOBACCO, ALCOHOL }
enum class TriggerCategory { STRESS, BOREDOM, SOCIAL, HABIT, OTHER }

data class CravingPrediction(
    val module: TrackedModule,
    val peakHourOfDay: Int,
    val peakDayOfWeek: DayOfWeek?,
    val confidence: Float, // 0..1, based on sample size and how peaked the distribution is
    val nextPredictedEpochMillis: Long?,
    val dominantTrigger: TriggerCategory?,
)

/**
 * Real, lightweight on-device pattern recognition: no ML model or cloud
 * inference, just frequency analysis over the user's own historical logs
 * (hour-of-day / day-of-week clustering, plus keyword-based trigger
 * detection). This is the honest scope of "predictive AI" that runs fully
 * offline on a phone without a trained model.
 */
class CravingPredictionEngine(
    private val cannabisRepository: CannabisRepository,
    private val tobaccoRepository: TobaccoRepository,
    private val alcoholRepository: AlcoholRepository,
    private val dopamineFocusRepository: DopamineFocusRepository,
) {
    private val zoneId = ZoneId.systemDefault()

    suspend fun predictAll(nowEpochMillis: Long = System.currentTimeMillis()): List<CravingPrediction> {
        val cannabisTimestamps = cannabisRepository.observeLogs().first().map { it.timestampEpochMillis }
        val tobaccoLogs = tobaccoRepository.observeLogs().first()
        val alcoholTimestamps = alcoholRepository.observeLogs().first().map { it.timestampEpochMillis }
        val whyReasons = dopamineFocusRepository.observeWhyPrompts().first().map { it.reason }

        val dominantTrigger = dominantTriggerFromText(whyReasons)
            ?: dominantTriggerFromTobaccoLogs(tobaccoLogs.mapNotNull { it.trigger })

        return listOfNotNull(
            predictFor(TrackedModule.CANNABIS, cannabisTimestamps, dominantTrigger, nowEpochMillis),
            predictFor(TrackedModule.TOBACCO, tobaccoLogs.map { it.timestampEpochMillis }, dominantTrigger, nowEpochMillis),
            predictFor(TrackedModule.ALCOHOL, alcoholTimestamps, dominantTrigger, nowEpochMillis),
        )
    }

    private fun predictFor(
        module: TrackedModule,
        timestamps: List<Long>,
        dominantTrigger: TriggerCategory?,
        nowEpochMillis: Long,
    ): CravingPrediction? {
        if (timestamps.size < MIN_SAMPLES) return null

        val zonedTimes = timestamps.map { Instant.ofEpochMilli(it).atZone(zoneId) }
        val hourCounts = zonedTimes.groupingBy { it.hour }.eachCount()
        val dayCounts = zonedTimes.groupingBy { it.dayOfWeek }.eachCount()

        val peakHour = hourCounts.maxByOrNull { it.value } ?: return null
        val peakDay = dayCounts.maxByOrNull { it.value }?.key

        val hourConcentration = peakHour.value.toFloat() / zonedTimes.size
        val confidence = (hourConcentration * (zonedTimes.size.coerceAtMost(20) / 20f)).coerceIn(0f, 1f)

        val next = nextOccurrence(peakDay, peakHour.key, nowEpochMillis)

        return CravingPrediction(
            module = module,
            peakHourOfDay = peakHour.key,
            peakDayOfWeek = peakDay,
            confidence = confidence,
            nextPredictedEpochMillis = next,
            dominantTrigger = dominantTrigger,
        )
    }

    private fun nextOccurrence(dayOfWeek: DayOfWeek?, hourOfDay: Int, nowEpochMillis: Long): Long? {
        if (dayOfWeek == null) return null
        var candidate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowEpochMillis), zoneId)
            .withHour(hourOfDay).withMinute(0).withSecond(0).withNano(0)
        while (candidate.dayOfWeek != dayOfWeek || candidate.toInstant().toEpochMilli() <= nowEpochMillis) {
            candidate = candidate.plusDays(1)
        }
        return candidate.toInstant().toEpochMilli()
    }

    private val stressKeywords = listOf("stress", "ansi", "nervos", "anxious", "worried", "preoccup")
    private val boredomKeywords = listOf("noia", "annoi", "bored", "niente da fare", "nothing to do")
    private val socialKeywords = listOf("amici", "social", "party", "festa", "friends", "uscita")

    private fun dominantTriggerFromText(reasons: List<String>): TriggerCategory? {
        if (reasons.isEmpty()) return null
        val lower = reasons.map { it.lowercase() }
        val stressCount = lower.count { text -> stressKeywords.any { text.contains(it) } }
        val boredomCount = lower.count { text -> boredomKeywords.any { text.contains(it) } }
        val socialCount = lower.count { text -> socialKeywords.any { text.contains(it) } }

        val best = listOf(
            TriggerCategory.STRESS to stressCount,
            TriggerCategory.BOREDOM to boredomCount,
            TriggerCategory.SOCIAL to socialCount,
        ).maxByOrNull { it.second }

        return if (best != null && best.second > 0) best.first else null
    }

    private fun dominantTriggerFromTobaccoLogs(triggers: List<String>): TriggerCategory? {
        if (triggers.isEmpty()) return null
        val mode = triggers.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: return null
        return when (mode) {
            "STRESS" -> TriggerCategory.STRESS
            "BOREDOM" -> TriggerCategory.BOREDOM
            "SOCIAL" -> TriggerCategory.SOCIAL
            "HABIT", "AFTER_MEAL" -> TriggerCategory.HABIT
            else -> TriggerCategory.OTHER
        }
    }

    companion object {
        private const val MIN_SAMPLES = 5
    }
}
