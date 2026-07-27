package com.dopamind.app.feature.cannabis.domain

/**
 * Pure Kotlin, no Android dependency (safe to reuse from a future KMP/iOS target).
 *
 * Informational-only harm-reduction heuristic — NOT medical dosing advice.
 * Numbers are conservative general guidance commonly used in harm-reduction
 * communities, not clinical thresholds.
 */
enum class EdiblesIntensity { MICRO, LOW, MODERATE, HIGH, VERY_HIGH }

data class EdiblesEstimate(
    val mgPerKg: Float,
    val intensity: EdiblesIntensity,
    val suggestedWaitMinutesBeforeRedose: Int,
    val expectedOnsetMinutesRange: IntRange,
)

object EdiblesCalculator {

    fun estimate(doseMg: Float, bodyWeightKg: Float, hasToleranceExperience: Boolean): EdiblesEstimate {
        require(bodyWeightKg > 0f) { "bodyWeightKg must be > 0" }
        val mgPerKg = doseMg / bodyWeightKg

        val toleranceShift = if (hasToleranceExperience) 0.02f else 0f
        val intensity = when {
            mgPerKg < 0.03f + toleranceShift -> EdiblesIntensity.MICRO
            mgPerKg < 0.07f + toleranceShift -> EdiblesIntensity.LOW
            mgPerKg < 0.15f + toleranceShift -> EdiblesIntensity.MODERATE
            mgPerKg < 0.30f + toleranceShift -> EdiblesIntensity.HIGH
            else -> EdiblesIntensity.VERY_HIGH
        }

        return EdiblesEstimate(
            mgPerKg = mgPerKg,
            intensity = intensity,
            // Edibles have delayed, variable onset — the #1 harm-reduction rule is
            // "wait it out" before taking more.
            suggestedWaitMinutesBeforeRedose = 120,
            expectedOnsetMinutesRange = 30..90,
        )
    }
}
