package com.dopamind.app.feature.nutrition.domain

import com.dopamind.app.feature.alcohol.domain.BiologicalSex
import com.dopamind.app.feature.profile.data.ActivityLevel
import kotlin.math.roundToInt

data class NutritionGoal(
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
)

/**
 * Pure Kotlin — no Android dependency.
 *
 * Mifflin-St Jeor BMR + activity-level TDEE, with a fixed default macro
 * split (30% protein / 40% carbs / 30% fat). This is a standard, widely-used
 * *estimate* for general population use — informational, not a personalized
 * medical/dietary prescription. Same non-clinical framing as the rest of the
 * app's calculators.
 */
object NutritionGoalCalculator {

    private const val PROTEIN_CALORIE_SHARE = 0.30
    private const val CARBS_CALORIE_SHARE = 0.40
    private const val FAT_CALORIE_SHARE = 0.30

    private const val CALORIES_PER_GRAM_PROTEIN = 4
    private const val CALORIES_PER_GRAM_CARBS = 4
    private const val CALORIES_PER_GRAM_FAT = 9

    fun calculateBmr(weightKg: Float, heightCm: Int, ageYears: Int, sex: BiologicalSex): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * ageYears
        return when (sex) {
            BiologicalSex.MALE -> base + 5
            BiologicalSex.FEMALE -> base - 161
            BiologicalSex.OTHER -> base - 78 // midpoint of the male/female offsets above
        }
    }

    fun calculateTdee(bmr: Double, activityLevel: ActivityLevel): Double = bmr * activityLevel.tdeeMultiplier

    fun calculateGoal(
        weightKg: Float,
        heightCm: Int,
        ageYears: Int,
        sex: BiologicalSex,
        activityLevel: ActivityLevel,
        manualCalorieOverride: Int? = null,
    ): NutritionGoal {
        val calories = manualCalorieOverride
            ?: calculateTdee(calculateBmr(weightKg, heightCm, ageYears, sex), activityLevel).roundToInt()

        return NutritionGoal(
            calories = calories,
            proteinGrams = (calories * PROTEIN_CALORIE_SHARE / CALORIES_PER_GRAM_PROTEIN).roundToInt(),
            carbsGrams = (calories * CARBS_CALORIE_SHARE / CALORIES_PER_GRAM_CARBS).roundToInt(),
            fatGrams = (calories * FAT_CALORIE_SHARE / CALORIES_PER_GRAM_FAT).roundToInt(),
        )
    }
}
