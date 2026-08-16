package com.dopamind.app.feature.profile.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.dopamind.app.feature.alcohol.domain.BiologicalSex
import kotlinx.coroutines.flow.Flow

enum class LanguagePreference { SYSTEM, IT, EN }

enum class ActivityLevel(val tdeeMultiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    VERY_ACTIVE(1.9),
}

private const val DEFAULT_HEIGHT_CM = 170f
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
    val heightCm: Float = DEFAULT_HEIGHT_CM,
    val ageYears: Int = DEFAULT_AGE_YEARS,
    val activityLevel: String = ActivityLevel.MODERATE.name,
    val dailyWaterGoalMl: Int = DEFAULT_WATER_GOAL_ML,
    val dailyCalorieGoalOverride: Int? = null,
)

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
            )
        )
    }

    suspend fun updateProfile(existing: UserProfileEntity, mutate: (UserProfileEntity) -> UserProfileEntity) {
        dao.upsert(mutate(existing))
    }
}
