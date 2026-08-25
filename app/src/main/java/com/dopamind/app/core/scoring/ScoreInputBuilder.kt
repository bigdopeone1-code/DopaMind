package com.dopamind.app.core.scoring

import com.dopamind.app.core.habit.BehaviorEventRepository
import com.dopamind.app.core.habit.HabitCategory
import com.dopamind.app.core.habit.toDomainOrNull
import com.dopamind.app.feature.dailyvibe.data.DailyVibeRepository
import com.dopamind.app.feature.profile.data.ProfileRepository
import com.dopamind.app.feature.recovery.data.RecoveryRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * The Room-aware seam between the repositories and the pure scoring layer.
 *
 * Everything Android-specific about producing a score lives here; everything
 * algorithmic lives in [DopaScoreEngine] and [BodyLoadCalculator], which know
 * nothing about Room, Context or coroutine dispatchers. That split is what
 * keeps the scoring logic testable on the JVM and portable later.
 */
class ScoreInputBuilder(
    private val behaviorEventRepository: BehaviorEventRepository,
    private val recoveryRepository: RecoveryRepository,
    private val dailyVibeRepository: DailyVibeRepository,
    private val profileRepository: ProfileRepository,
) {

    fun todayEpochDay(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    suspend fun build(config: ScoringConfig = ScoringConfig.DEFAULT): ScoreInput {
        val today = todayEpochDay()
        val startEpochDay = today - config.windowDays + 1
        val sinceMillis = startEpochDay * MILLIS_PER_DAY

        val events = behaviorEventRepository.observeSince(sinceMillis).first().mapNotNull { it.toDomainOrNull() }
        val sleepLogs = recoveryRepository.observeSleepLogsSince(startEpochDay).first()
        val vibeLogs = dailyVibeRepository.observeSince(startEpochDay).first()
        val trackedCategories = profileRepository.getProfileOnce()?.trackedCategories()
            ?: HabitCategory.DEFAULT_SELECTION

        val eventsByDay = events.groupBy { it.timestampEpochMillis / MILLIS_PER_DAY }

        val days = (startEpochDay..today).map { day ->
            val dayEvents = eventsByDay[day].orEmpty()

            // Raw amounts are summed per category first, then normalised once —
            // normalising each event separately would clip every single log at
            // 1.0 and lose the difference between one drink and six.
            val loadByCategory = dayEvents
                .groupBy { it.category }
                .mapValues { (category, categoryEvents) ->
                    category.normalisedLoad(categoryEvents.sumOf { it.amount.toDouble() }.toFloat())
                }

            val sleep = sleepLogs.firstOrNull { it.dateEpochDay == day }
            val vibe = vibeLogs.firstOrNull { it.dateEpochDay == day }

            DayAggregate(
                epochDay = day,
                loadByCategory = loadByCategory,
                sleepHours = sleep?.hours ?: vibe?.sleepHours,
                sleepQuality = sleep?.quality,
                // Daily Vibe stores a 0..4 slider index; the scoring layer works
                // on a 1..5 self-report scale, so shift rather than rescale.
                moodIndex = vibe?.moodEnergyIndex?.plus(1)
                    ?: dayEvents.mapNotNull { it.mood }.averageOrNull()?.toInt(),
                stressIndex = dayEvents.mapNotNull { it.stress }.averageOrNull()?.toInt(),
            )
        }

        return ScoreInput(todayEpochDay = today, days = days, trackedCategories = trackedCategories)
    }
}

private fun List<Int>.averageOrNull(): Double? = if (isEmpty()) null else average()
