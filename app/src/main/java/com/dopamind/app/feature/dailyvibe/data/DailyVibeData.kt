package com.dopamind.app.feature.dailyvibe.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** One check-in per calendar day (epoch day, local time), enforced by the primary key. */
@Entity(tableName = "daily_vibe_checkins")
data class DailyVibeEntity(
    @PrimaryKey val dateEpochDay: Long,
    val moodEnergyIndex: Int, // 0..4, index into the mood/energy slider steps
    val consumedModulesCsv: String, // comma-joined module ids toggled as "consumed yesterday"
    val sleepHours: Float?,
    val note: String?,
    val completedAtEpochMillis: Long,
)

@Dao
interface DailyVibeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyVibeEntity)

    @Query("SELECT * FROM daily_vibe_checkins WHERE dateEpochDay = :dateEpochDay")
    suspend fun getForDate(dateEpochDay: Long): DailyVibeEntity?

    @Query("SELECT * FROM daily_vibe_checkins ORDER BY dateEpochDay DESC")
    fun observeAll(): Flow<List<DailyVibeEntity>>

    @Query("SELECT * FROM daily_vibe_checkins WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay ASC")
    fun observeSince(sinceEpochDay: Long): Flow<List<DailyVibeEntity>>
}

class DailyVibeRepository(private val dao: DailyVibeDao) {
    fun observeAll(): Flow<List<DailyVibeEntity>> = dao.observeAll()
    fun observeSince(sinceEpochDay: Long): Flow<List<DailyVibeEntity>> = dao.observeSince(sinceEpochDay)
    suspend fun getForDate(dateEpochDay: Long): DailyVibeEntity? = dao.getForDate(dateEpochDay)

    suspend fun submitCheckIn(
        dateEpochDay: Long,
        moodEnergyIndex: Int,
        consumedModules: List<String>,
        sleepHours: Float?,
        note: String?,
        completedAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        dao.upsert(
            DailyVibeEntity(
                dateEpochDay = dateEpochDay,
                moodEnergyIndex = moodEnergyIndex,
                consumedModulesCsv = consumedModules.joinToString(","),
                sleepHours = sleepHours,
                note = note,
                completedAtEpochMillis = completedAtEpochMillis,
            )
        )
    }

    /** Consecutive days (ending today or yesterday) with a completed check-in. */
    suspend fun currentStreak(todayEpochDay: Long): Int {
        var streak = 0
        var day = todayEpochDay
        // Allow "today not yet done" without breaking a streak built through yesterday.
        if (getForDate(day) == null) day -= 1
        while (getForDate(day) != null) {
            streak++
            day -= 1
        }
        return streak
    }
}

fun DailyVibeEntity.consumedModules(): List<String> =
    if (consumedModulesCsv.isBlank()) emptyList() else consumedModulesCsv.split(",")
