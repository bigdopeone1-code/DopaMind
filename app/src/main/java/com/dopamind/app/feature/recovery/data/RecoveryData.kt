package com.dopamind.app.feature.recovery.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class MunchiesSource { MANUAL, VISION_SCAN }

enum class FoodCategory { SWEET, SALTY_SNACK, FRIED, FAST_FOOD, FRUIT_VEG, OTHER }

@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey val dateEpochDay: Long,
    val hours: Float,
    val quality: Int, // 1-5
    val note: String?,
)

@Entity(tableName = "munchies_logs")
data class MunchiesLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMillis: Long,
    val foodDescription: String,
    val junkScore: Int, // 0-100
    val source: String, // MunchiesSource.name
    val foodCategory: String = FoodCategory.OTHER.name,
)

@Entity(tableName = "sos_sessions")
data class SosSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMillis: Long,
)

@Dao
interface RecoveryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSleepLog(entity: SleepLogEntity)

    @Query("SELECT * FROM sleep_logs ORDER BY dateEpochDay DESC")
    fun observeSleepLogs(): Flow<List<SleepLogEntity>>

    @Query("SELECT * FROM sleep_logs WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay ASC")
    fun observeSleepLogsSince(sinceEpochDay: Long): Flow<List<SleepLogEntity>>

    @Insert
    suspend fun insertMunchiesLog(entity: MunchiesLogEntity): Long

    @Query("SELECT * FROM munchies_logs ORDER BY timestampEpochMillis DESC")
    fun observeMunchiesLogs(): Flow<List<MunchiesLogEntity>>

    @Insert
    suspend fun insertSosSession(entity: SosSessionEntity): Long

    @Query("SELECT * FROM sos_sessions ORDER BY timestampEpochMillis DESC")
    fun observeSosSessions(): Flow<List<SosSessionEntity>>
}

class RecoveryRepository(private val dao: RecoveryDao) {
    fun observeSleepLogs(): Flow<List<SleepLogEntity>> = dao.observeSleepLogs()
    fun observeSleepLogsSince(sinceEpochDay: Long): Flow<List<SleepLogEntity>> = dao.observeSleepLogsSince(sinceEpochDay)
    fun observeMunchiesLogs(): Flow<List<MunchiesLogEntity>> = dao.observeMunchiesLogs()
    fun observeSosSessions(): Flow<List<SosSessionEntity>> = dao.observeSosSessions()

    suspend fun logSleep(dateEpochDay: Long, hours: Float, quality: Int, note: String?) {
        dao.upsertSleepLog(SleepLogEntity(dateEpochDay = dateEpochDay, hours = hours, quality = quality, note = note))
    }

    suspend fun logMunchies(
        foodDescription: String,
        junkScore: Int,
        source: MunchiesSource,
        foodCategory: FoodCategory = FoodCategory.OTHER,
        timestampEpochMillis: Long = System.currentTimeMillis(),
    ) {
        dao.insertMunchiesLog(
            MunchiesLogEntity(
                timestampEpochMillis = timestampEpochMillis,
                foodDescription = foodDescription,
                junkScore = junkScore,
                source = source.name,
                foodCategory = foodCategory.name,
            )
        )
    }

    suspend fun logSosUsage(timestampEpochMillis: Long = System.currentTimeMillis()) {
        dao.insertSosSession(SosSessionEntity(timestampEpochMillis = timestampEpochMillis))
    }
}
