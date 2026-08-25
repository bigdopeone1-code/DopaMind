package com.dopamind.app.core.habit

import com.dopamind.app.R

/** How a category is quantified — decides which input the Add Event sheet shows. */
enum class MeasurementKind { COUNT, DURATION_MINUTES }

/**
 * The body systems the Home screen surfaces. These are **behavioural exposure
 * indices, not organ health readings** — a value here says "how much of this
 * week's logged load lands on behaviours associated with this system", nothing
 * about the physical state of the organ. Copy must never imply a clinical
 * measurement; see [com.dopamind.app.core.scoring.BodyLoadCalculator].
 */
enum class BodySystem { BRAIN, HEART, LUNGS, LIVER, RECOVERY }

/**
 * The tracked-behaviour taxonomy. Deliberately open and user-selected: the
 * onboarding lets each user pick the categories that are relevant to them and
 * the Daily Log renders only those, so the app never assumes a given user has
 * a given habit.
 *
 * Adding a category is a one-line change here plus its two string resources —
 * nothing else in the scoring, logging or UI layers hardcodes the list.
 *
 * @param measurement whether events are counted or timed.
 * @param dailyReferenceLoad the amount treated as one full day of load for this
 *   category (i.e. the normaliser that maps a raw amount onto the 0..1 load
 *   scale). A design constant chosen for scoring stability, not a health
 *   threshold or a recommended limit.
 * @param quickAmounts the one-tap presets offered in the Add Event sheet.
 * @param systemLoad how a full day of this category distributes onto body
 *   systems (0..1 per system). Heuristic product weights, not clinical data.
 */
enum class HabitCategory(
    val labelRes: Int,
    val unitRes: Int,
    val measurement: MeasurementKind,
    val dailyReferenceLoad: Float,
    val quickAmounts: List<Int>,
    val systemLoad: Map<BodySystem, Float>,
) {
    NICOTINE(
        labelRes = R.string.habit_nicotine,
        unitRes = R.string.habit_unit_count,
        measurement = MeasurementKind.COUNT,
        dailyReferenceLoad = 15f,
        quickAmounts = listOf(1, 2, 5),
        systemLoad = mapOf(BodySystem.LUNGS to 1.0f, BodySystem.HEART to 0.6f, BodySystem.BRAIN to 0.3f, BodySystem.RECOVERY to 0.3f),
    ),
    ALCOHOL(
        labelRes = R.string.habit_alcohol,
        unitRes = R.string.habit_unit_drinks,
        measurement = MeasurementKind.COUNT,
        dailyReferenceLoad = 4f,
        quickAmounts = listOf(1, 2, 3),
        systemLoad = mapOf(BodySystem.LIVER to 1.0f, BodySystem.BRAIN to 0.6f, BodySystem.RECOVERY to 0.7f, BodySystem.HEART to 0.3f),
    ),
    CANNABIS(
        labelRes = R.string.habit_cannabis,
        unitRes = R.string.habit_unit_count,
        measurement = MeasurementKind.COUNT,
        dailyReferenceLoad = 3f,
        quickAmounts = listOf(1, 2, 3),
        systemLoad = mapOf(BodySystem.BRAIN to 0.8f, BodySystem.LUNGS to 0.5f, BodySystem.RECOVERY to 0.4f),
    ),
    CAFFEINE(
        labelRes = R.string.habit_caffeine,
        unitRes = R.string.habit_unit_servings,
        measurement = MeasurementKind.COUNT,
        dailyReferenceLoad = 4f,
        quickAmounts = listOf(1, 2, 3),
        systemLoad = mapOf(BodySystem.HEART to 0.6f, BodySystem.RECOVERY to 0.5f, BodySystem.BRAIN to 0.2f),
    ),
    ENERGY_DRINKS(
        labelRes = R.string.habit_energy_drinks,
        unitRes = R.string.habit_unit_servings,
        measurement = MeasurementKind.COUNT,
        dailyReferenceLoad = 2f,
        quickAmounts = listOf(1, 2),
        systemLoad = mapOf(BodySystem.HEART to 0.8f, BodySystem.RECOVERY to 0.6f, BodySystem.BRAIN to 0.3f),
    ),
    SOCIAL_MEDIA(
        labelRes = R.string.habit_social_media,
        unitRes = R.string.habit_unit_minutes,
        measurement = MeasurementKind.DURATION_MINUTES,
        dailyReferenceLoad = 180f,
        quickAmounts = listOf(15, 30, 60),
        systemLoad = mapOf(BodySystem.BRAIN to 1.0f, BodySystem.RECOVERY to 0.4f),
    ),
    GAMING(
        labelRes = R.string.habit_gaming,
        unitRes = R.string.habit_unit_minutes,
        measurement = MeasurementKind.DURATION_MINUTES,
        dailyReferenceLoad = 180f,
        quickAmounts = listOf(30, 60, 120),
        systemLoad = mapOf(BodySystem.BRAIN to 0.8f, BodySystem.RECOVERY to 0.5f),
    ),
    PORNOGRAPHY(
        labelRes = R.string.habit_pornography,
        unitRes = R.string.habit_unit_count,
        measurement = MeasurementKind.COUNT,
        dailyReferenceLoad = 2f,
        quickAmounts = listOf(1, 2),
        systemLoad = mapOf(BodySystem.BRAIN to 0.9f, BodySystem.RECOVERY to 0.3f),
    ),
    GAMBLING(
        labelRes = R.string.habit_gambling,
        unitRes = R.string.habit_unit_count,
        measurement = MeasurementKind.COUNT,
        dailyReferenceLoad = 2f,
        quickAmounts = listOf(1, 2),
        systemLoad = mapOf(BodySystem.BRAIN to 1.0f, BodySystem.RECOVERY to 0.4f),
    ),
    LATE_NIGHT_SCREEN(
        labelRes = R.string.habit_late_night_screen,
        unitRes = R.string.habit_unit_minutes,
        measurement = MeasurementKind.DURATION_MINUTES,
        dailyReferenceLoad = 90f,
        quickAmounts = listOf(15, 30, 60),
        systemLoad = mapOf(BodySystem.RECOVERY to 1.0f, BodySystem.BRAIN to 0.6f),
    ),
    SLEEP_DEPRIVATION(
        labelRes = R.string.habit_sleep_deprivation,
        unitRes = R.string.habit_unit_count,
        measurement = MeasurementKind.COUNT,
        dailyReferenceLoad = 1f,
        quickAmounts = listOf(1),
        systemLoad = mapOf(BodySystem.RECOVERY to 1.0f, BodySystem.BRAIN to 0.7f, BodySystem.HEART to 0.3f),
    ),
    OVEREATING(
        labelRes = R.string.habit_overeating,
        unitRes = R.string.habit_unit_count,
        measurement = MeasurementKind.COUNT,
        dailyReferenceLoad = 2f,
        quickAmounts = listOf(1, 2),
        systemLoad = mapOf(BodySystem.LIVER to 0.7f, BodySystem.RECOVERY to 0.5f, BodySystem.HEART to 0.3f),
    ),
    OTHER(
        labelRes = R.string.habit_other,
        unitRes = R.string.habit_unit_count,
        measurement = MeasurementKind.COUNT,
        dailyReferenceLoad = 3f,
        quickAmounts = listOf(1, 2),
        systemLoad = mapOf(BodySystem.BRAIN to 0.4f, BodySystem.RECOVERY to 0.3f),
    );

    /** Maps a raw logged amount onto the shared 0..1 daily load scale. */
    fun normalisedLoad(amount: Float): Float = (amount / dailyReferenceLoad).coerceIn(0f, 1f)

    companion object {
        /** Categories offered by default when a user skips the onboarding picker. */
        val DEFAULT_SELECTION = listOf(NICOTINE, ALCOHOL, SOCIAL_MEDIA, LATE_NIGHT_SCREEN)

        fun fromNameOrNull(name: String): HabitCategory? = entries.firstOrNull { it.name == name }
    }
}
