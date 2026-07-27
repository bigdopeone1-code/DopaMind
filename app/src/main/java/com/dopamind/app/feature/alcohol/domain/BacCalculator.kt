package com.dopamind.app.feature.alcohol.domain

/**
 * Pure Kotlin — no Android dependency.
 *
 * Widmark-formula BAC estimate. This is a rough approximation for
 * entertainment/awareness purposes, NOT a medical or legal instrument —
 * individual metabolism varies significantly. The app must never present
 * this as a guarantee of legal sobriety.
 */
enum class BiologicalSex(val widmarkR: Double) {
    MALE(0.68),
    FEMALE(0.55),
    OTHER(0.615), // average of the two standard constants when unspecified
}

data class DrinkInput(
    val gramsAlcohol: Double,
    val timestampEpochMillis: Long,
)

data class BacResult(
    val bacGramsPerLiter: Double,
    val gramsAlcoholTotal: Double,
    val hoursSinceFirstDrink: Double,
    val isStillRising: Boolean,
)

object BacCalculator {

    private const val ETHANOL_DENSITY_G_PER_ML = 0.789
    // Average elimination rate, g/L per hour (commonly cited range 0.10–0.20).
    private const val ELIMINATION_RATE_G_PER_L_PER_HOUR = 0.15
    // Absorption is not instantaneous; treat BAC as still rising for this long after the last drink.
    private const val ABSORPTION_WINDOW_MINUTES = 45

    fun gramsOfAlcohol(volumeMl: Int, abvPercent: Float): Double =
        volumeMl * (abvPercent / 100.0) * ETHANOL_DENSITY_G_PER_ML

    fun estimate(
        drinks: List<DrinkInput>,
        bodyWeightKg: Double,
        sex: BiologicalSex,
        nowEpochMillis: Long,
    ): BacResult {
        if (drinks.isEmpty() || bodyWeightKg <= 0.0) {
            return BacResult(0.0, 0.0, 0.0, isStillRising = false)
        }

        val totalGrams = drinks.sumOf { it.gramsAlcohol }
        val firstDrinkAt = drinks.minOf { it.timestampEpochMillis }
        val lastDrinkAt = drinks.maxOf { it.timestampEpochMillis }
        val hoursSinceFirst = (nowEpochMillis - firstDrinkAt) / 3_600_000.0

        val peakBac = totalGrams / (sex.widmarkR * bodyWeightKg)
        val eliminated = ELIMINATION_RATE_G_PER_L_PER_HOUR * hoursSinceFirst
        val currentBac = (peakBac - eliminated).coerceAtLeast(0.0)

        val minutesSinceLastDrink = (nowEpochMillis - lastDrinkAt) / 60_000.0
        val isStillRising = minutesSinceLastDrink in 0.0..ABSORPTION_WINDOW_MINUTES.toDouble()

        return BacResult(
            bacGramsPerLiter = currentBac,
            gramsAlcoholTotal = totalGrams,
            hoursSinceFirstDrink = hoursSinceFirst,
            isStillRising = isStillRising,
        )
    }

    /** Legal driving thresholds, grams/liter — Italy by default. */
    object LegalThresholds {
        const val ITALY_GENERAL_GL = 0.5
        const val ITALY_NOVICE_OR_PROFESSIONAL_GL = 0.0
    }

    data class DriveAssessment(val canLikelyDrive: Boolean, val marginGramsPerLiter: Double, val hoursUntilLegalEstimate: Double)

    fun assessDriving(bac: BacResult, thresholdGramsPerLiter: Double = LegalThresholds.ITALY_GENERAL_GL): DriveAssessment {
        val margin = thresholdGramsPerLiter - bac.bacGramsPerLiter
        val hoursUntilLegal = if (margin >= 0) 0.0 else (-margin) / ELIMINATION_RATE_G_PER_L_PER_HOUR
        return DriveAssessment(
            canLikelyDrive = margin >= 0 && !bac.isStillRising,
            marginGramsPerLiter = margin,
            hoursUntilLegalEstimate = hoursUntilLegal,
        )
    }
}
