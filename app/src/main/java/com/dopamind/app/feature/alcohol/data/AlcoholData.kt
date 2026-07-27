package com.dopamind.app.feature.alcohol.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class DrinkType(val defaultVolumeMl: Int, val defaultAbvPercent: Float) {
    BEER(330, 5.0f),
    WINE(150, 12.5f),
    SPIRIT_SHOT(40, 40.0f),
    COCKTAIL(200, 15.0f),
    OTHER(330, 5.0f),
}

@Entity(tableName = "drink_logs")
data class DrinkLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMillis: Long,
    val drinkType: String, // DrinkType.name
    val volumeMl: Int,
    val abvPercent: Float,
    val priceEuros: Float?,
    val venue: String?,
)

@Dao
interface AlcoholDao {
    @Insert
    suspend fun insertLog(log: DrinkLogEntity): Long

    @Query("DELETE FROM drink_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("SELECT * FROM drink_logs ORDER BY timestampEpochMillis DESC")
    fun observeLogs(): Flow<List<DrinkLogEntity>>

    @Query("SELECT * FROM drink_logs WHERE timestampEpochMillis >= :sinceEpochMillis ORDER BY timestampEpochMillis ASC")
    fun observeLogsSince(sinceEpochMillis: Long): Flow<List<DrinkLogEntity>>
}

class AlcoholRepository(private val dao: AlcoholDao) {
    fun observeLogs(): Flow<List<DrinkLogEntity>> = dao.observeLogs()
    fun observeLogsSince(sinceEpochMillis: Long): Flow<List<DrinkLogEntity>> = dao.observeLogsSince(sinceEpochMillis)

    suspend fun logDrink(
        drinkType: DrinkType,
        volumeMl: Int,
        abvPercent: Float,
        priceEuros: Float?,
        venue: String?,
        timestampEpochMillis: Long = System.currentTimeMillis(),
    ) {
        dao.insertLog(
            DrinkLogEntity(
                timestampEpochMillis = timestampEpochMillis,
                drinkType = drinkType.name,
                volumeMl = volumeMl,
                abvPercent = abvPercent,
                priceEuros = priceEuros,
                venue = venue,
            )
        )
    }

    suspend fun deleteLog(id: Long) = dao.deleteLog(id)
}
