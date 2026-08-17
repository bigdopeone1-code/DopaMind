package com.dopamind.app.feature.nutrition.domain

import com.dopamind.app.feature.alcohol.domain.BiologicalSex
import com.dopamind.app.feature.profile.data.ActivityLevel
import com.dopamind.app.feature.profile.data.NutritionGoalType
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

    // ~0.5kg/week of fat is roughly a 500kcal/day deficit (7700kcal per kg of fat / 7 days).
    // Mirrors the pace most goal-tracking apps (incl. Yazio) default to.
    private const val WEIGHT_LOSS_DAILY_DEFICIT = 500
    private const val WEIGHT_GAIN_DAILY_SURPLUS = 300

    // Never recommend below this floor — informational safety rail, not a clinical minimum.
    private const val MIN_SAFE_DAILY_CALORIES = 1200

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
        goalType: NutritionGoalType = NutritionGoalType.MAINTAIN,
        manualCalorieOverride: Int? = null,
    ): NutritionGoal {
        val calories = manualCalorieOverride ?: run {
            val tdee = calculateTdee(calculateBmr(weightKg, heightCm, ageYears, sex), activityLevel)
            val adjusted = when (goalType) {
                NutritionGoalType.LOSE_WEIGHT -> tdee - WEIGHT_LOSS_DAILY_DEFICIT
                NutritionGoalType.MAINTAIN -> tdee
                NutritionGoalType.GAIN_WEIGHT -> tdee + WEIGHT_GAIN_DAILY_SURPLUS
            }
            adjusted.roundToInt().coerceAtLeast(MIN_SAFE_DAILY_CALORIES)
        }

        return NutritionGoal(
            calories = calories,
            proteinGrams = (calories * PROTEIN_CALORIE_SHARE / CALORIES_PER_GRAM_PROTEIN).roundToInt(),
            carbsGrams = (calories * CARBS_CALORIE_SHARE / CALORIES_PER_GRAM_CARBS).roundToInt(),
            fatGrams = (calories * FAT_CALORIE_SHARE / CALORIES_PER_GRAM_FAT).roundToInt(),
        )
    }
}
