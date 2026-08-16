package com.dopamind.app.feature.nutrition.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

@Entity(tableName = "food_logs")
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMillis: Long,
    val mealType: String, // MealType.name
    val name: String,
    val calories: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
)

@Entity(tableName = "weight_logs")
data class WeightLogEntity(
    @PrimaryKey val dateEpochDay: Long,
    val weightKg: Float,
)

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMillis: Long,
    val amountMl: Int,
)

@Entity(tableName = "fasting_sessions")
data class FastingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMillis: Long,
    val targetHours: Int,
    val endEpochMillis: Long?,
    val completed: Boolean,
)

@Dao
interface NutritionDao {
    @Insert
    suspend fun insertFood(entity: FoodLogEntity): Long

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteFood(id: Long)

    @Query("SELECT * FROM food_logs ORDER BY timestampEpochMillis DESC")
    fun observeFoodLogs(): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE timestampEpochMillis >= :sinceEpochMillis ORDER BY timestampEpochMillis DESC")
    fun observeFoodLogsSince(sinceEpochMillis: Long): Flow<List<FoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeight(entity: WeightLogEntity)

    @Query("SELECT * FROM weight_logs ORDER BY dateEpochDay DESC")
    fun observeWeightLogs(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay ASC")
    fun observeWeightLogsSince(sinceEpochDay: Long): Flow<List<WeightLogEntity>>

    @Insert
    suspend fun insertWater(entity: WaterLogEntity): Long

    @Query("SELECT * FROM water_logs WHERE timestampEpochMillis >= :sinceEpochMillis ORDER BY timestampEpochMillis DESC")
    fun observeWaterLogsSince(sinceEpochMillis: Long): Flow<List<WaterLogEntity>>

    @Insert
    suspend fun insertFastingSession(entity: FastingSessionEntity): Long

    @Query("UPDATE fasting_sessions SET endEpochMillis = :endEpochMillis, completed = :completed WHERE id = :id")
    suspend fun endFastingSession(id: Long, endEpochMillis: Long, completed: Boolean)

    @Query("SELECT * FROM fasting_sessions WHERE endEpochMillis IS NULL ORDER BY startEpochMillis DESC LIMIT 1")
    fun observeActiveFastingSession(): Flow<FastingSessionEntity?>
}

class NutritionRepository(private val dao: NutritionDao) {
    fun observeFoodLogs(): Flow<List<FoodLogEntity>> = dao.observeFoodLogs()
    fun observeFoodLogsSince(sinceEpochMillis: Long): Flow<List<FoodLogEntity>> = dao.observeFoodLogsSince(sinceEpochMillis)
    fun observeWeightLogs(): Flow<List<WeightLogEntity>> = dao.observeWeightLogs()
    fun observeWeightLogsSince(sinceEpochDay: Long): Flow<List<WeightLogEntity>> = dao.observeWeightLogsSince(sinceEpochDay)
    fun observeWaterLogsSince(sinceEpochMillis: Long): Flow<List<WaterLogEntity>> = dao.observeWaterLogsSince(sinceEpochMillis)
    fun observeActiveFastingSession(): Flow<FastingSessionEntity?> = dao.observeActiveFastingSession()

    suspend fun logFood(mealType: MealType, name: String, calories: Int, proteinGrams: Float, carbsGrams: Float, fatGrams: Float, timestampEpochMillis: Long = System.currentTimeMillis()) {
        dao.insertFood(
            FoodLogEntity(
                timestampEpochMillis = timestampEpochMillis,
                mealType = mealType.name,
                name = name,
                calories = calories,
                proteinGrams = proteinGrams,
                carbsGrams = carbsGrams,
                fatGrams = fatGrams,
            )
        )
    }

    suspend fun deleteFood(id: Long) = dao.deleteFood(id)

    suspend fun logWeight(dateEpochDay: Long, weightKg: Float) {
        dao.upsertWeight(WeightLogEntity(dateEpochDay = dateEpochDay, weightKg = weightKg))
    }

    suspend fun logWater(amountMl: Int, timestampEpochMillis: Long = System.currentTimeMillis()) {
        dao.insertWater(WaterLogEntity(timestampEpochMillis = timestampEpochMillis, amountMl = amountMl))
    }

    suspend fun startFasting(targetHours: Int, startEpochMillis: Long = System.currentTimeMillis()) {
        dao.insertFastingSession(FastingSessionEntity(startEpochMillis = startEpochMillis, targetHours = targetHours, endEpochMillis = null, completed = false))
    }

    suspend fun endFasting(session: FastingSessionEntity, completed: Boolean, endEpochMillis: Long = System.currentTimeMillis()) {
        dao.endFastingSession(session.id, endEpochMillis, completed)
    }
}
