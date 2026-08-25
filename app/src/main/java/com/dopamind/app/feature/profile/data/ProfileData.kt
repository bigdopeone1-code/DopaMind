package com.dopamind.app.feature.profile.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.dopamind.app.core.habit.HabitCategory
import com.dopamind.app.feature.alcohol.domain.BiologicalSex
import kotlinx.coroutines.flow.Flow

enum class LanguagePreference { SYSTEM, IT, EN }

/** How often the user engages in a given habit — shared by cannabis/alcohol onboarding questions. */
enum class UseFrequency { NEVER, RARE, OCCASIONAL, REGULAR }

enum class SmokingStatus { NEVER, OCCASIONAL, REGULAR, HEAVY }

/**
 * Onboarding only offers 4 buckets (see OnboardingScreen's activity step);
 * the TDEE multiplier is added here for NutritionGoalCalculator's BMR x
 * activity-level math. "ACTIVE" doubles as the traditional "moderately
 * active" tier since there's no separate 5th bucket in the UI.
 */
enum class ActivityLevel(val tdeeMultiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    ACTIVE(1.55),
    VERY_ACTIVE(1.725),
}

/**
 * The Yazio-style "what's your goal?" onboarding question. Drives the daily
 * calorie target in NutritionGoalCalculator (deficit for LOSE_WEIGHT,
 * surplus for GAIN_WEIGHT) — informational estimate, not a medical plan.
 */
enum class NutritionGoalType { LOSE_WEIGHT, MAINTAIN, GAIN_WEIGHT }

private const val DEFAULT_AGE_YEARS = 30
private const val DEFAULT_WATER_GOAL_ML = 2000

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String,
    val weightKg: Float,
    val biologicalSex: String, // BiologicalSex.name
    val languagePreference: String, // LanguagePreference.name
    val notificationsEnabled: Boolean,
    val dailyReminderHour: Int, // 0..23
    val onboardingCompletedAtEpochMillis: Long,
    val immersiveModeEnabled: Boolean = false,
    val heightCm: Int = 170,
    val smokingStatus: String = SmokingStatus.NEVER.name,
    val cannabisUseFrequency: String = UseFrequency.NEVER.name,
    val alcoholUseFrequency: String = UseFrequency.NEVER.name,
    val activityLevel: String = ActivityLevel.LIGHT.name,
    // Not collected during onboarding (keeps it short) — editable in Profile, used by the
    // Nutrition module's calorie/macro goal calculator.
    val ageYears: Int = DEFAULT_AGE_YEARS,
    val dailyWaterGoalMl: Int = DEFAULT_WATER_GOAL_ML,
    val dailyCalorieGoalOverride: Int? = null,
    val nutritionGoalType: String = NutritionGoalType.MAINTAIN.name,
    val targetWeightKg: Float? = null,
    /**
     * Comma-joined [HabitCategory] names the user chose to track. Empty means
     * "not chosen yet" and callers fall back to [HabitCategory.DEFAULT_SELECTION];
     * it is deliberately not pre-filled, so the Daily Log never assumes a habit
     * the user never told us about.
     */
    val trackedCategoriesCsv: String = "",
)

/** The user's chosen categories, falling back to the default set when unset. */
fun UserProfileEntity.trackedCategories(): List<HabitCategory> =
    trackedCategoriesCsv.split(",")
        .mapNotNull { HabitCategory.fromNameOrNull(it.trim()) }
        .ifEmpty { HabitCategory.DEFAULT_SELECTION }

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileOnce(): UserProfileEntity?
}

class ProfileRepository(private val dao: ProfileDao) {
    fun observeProfile(): Flow<UserProfileEntity?> = dao.observeProfile()
    suspend fun getProfileOnce(): UserProfileEntity? = dao.getProfileOnce()

    suspend fun completeOnboarding(
        displayName: String,
        weightKg: Float,
        sex: BiologicalSex,
        language: LanguagePreference,
        notificationsEnabled: Boolean,
        dailyReminderHour: Int,
        heightCm: Int = 170,
        smokingStatus: SmokingStatus = SmokingStatus.NEVER,
        cannabisUseFrequency: UseFrequency = UseFrequency.NEVER,
        alcoholUseFrequency: UseFrequency = UseFrequency.NEVER,
        activityLevel: ActivityLevel = ActivityLevel.LIGHT,
        nutritionGoalType: NutritionGoalType = NutritionGoalType.MAINTAIN,
        targetWeightKg: Float? = null,
        trackedCategories: List<HabitCategory> = emptyList(),
    ) {
        dao.upsert(
            UserProfileEntity(
                displayName = displayName,
                weightKg = weightKg,
                biologicalSex = sex.name,
                languagePreference = language.name,
                notificationsEnabled = notificationsEnabled,
                dailyReminderHour = dailyReminderHour,
                onboardingCompletedAtEpochMillis = System.currentTimeMillis(),
                heightCm = heightCm,
                smokingStatus = smokingStatus.name,
                cannabisUseFrequency = cannabisUseFrequency.name,
                alcoholUseFrequency = alcoholUseFrequency.name,
                activityLevel = activityLevel.name,
                nutritionGoalType = nutritionGoalType.name,
                targetWeightKg = targetWeightKg,
                trackedCategoriesCsv = trackedCategories.joinToString(",") { it.name },
            )
        )
    }

    suspend fun updateProfile(existing: UserProfileEntity, mutate: (UserProfileEntity) -> UserProfileEntity) {
        dao.upsert(mutate(existing))
    }
}
