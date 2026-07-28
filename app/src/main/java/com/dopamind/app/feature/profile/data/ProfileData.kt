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

/** How often the user engages in a given habit — shared by cannabis/alcohol onboarding questions. */
enum class UseFrequency { NEVER, RARE, OCCASIONAL, REGULAR }

enum class SmokingStatus { NEVER, OCCASIONAL, REGULAR, HEAVY }

enum class ActivityLevel { SEDENTARY, LIGHT, ACTIVE, VERY_ACTIVE }

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
        heightCm: Int = 170,
        smokingStatus: SmokingStatus = SmokingStatus.NEVER,
        cannabisUseFrequency: UseFrequency = UseFrequency.NEVER,
        alcoholUseFrequency: UseFrequency = UseFrequency.NEVER,
        activityLevel: ActivityLevel = ActivityLevel.LIGHT,
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
            )
        )
    }

    suspend fun updateProfile(existing: UserProfileEntity, mutate: (UserProfileEntity) -> UserProfileEntity) {
        dao.upsert(mutate(existing))
    }
}
