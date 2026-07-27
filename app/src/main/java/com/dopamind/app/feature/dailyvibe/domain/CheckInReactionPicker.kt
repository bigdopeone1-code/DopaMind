package com.dopamind.app.feature.dailyvibe.domain

import java.time.DayOfWeek

enum class CheckInReaction { WEEKEND_WARRIOR, CLEAN_DAY, NIGHT_OWL, ENERGIZED, LOW_BATTERY, BALANCED }

/** Pure Kotlin — no Android dependency. Picks the playful reveal-screen reaction. */
object CheckInReactionPicker {
    fun pick(
        dayOfWeek: DayOfWeek,
        moodEnergyIndex: Int,
        consumedModulesCount: Int,
        sleepHours: Float?,
    ): CheckInReaction = when {
        consumedModulesCount == 0 -> CheckInReaction.CLEAN_DAY
        (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) && consumedModulesCount >= 2 ->
            CheckInReaction.WEEKEND_WARRIOR
        sleepHours != null && sleepHours < 5f -> CheckInReaction.NIGHT_OWL
        moodEnergyIndex >= 3 -> CheckInReaction.ENERGIZED
        moodEnergyIndex <= 1 -> CheckInReaction.LOW_BATTERY
        else -> CheckInReaction.BALANCED
    }
}
