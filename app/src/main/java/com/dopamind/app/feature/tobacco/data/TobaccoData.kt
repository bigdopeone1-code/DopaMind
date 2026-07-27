package com.dopamind.app.feature.tobacco.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class TobaccoProductType { CIGARETTE, IQOS_STICK, VAPE_PUFF, OTHER }
enum class TobaccoTrigger { BOREDOM, STRESS, SOCIAL, HABIT, AFTER_MEAL, ALCOHOL, OTHER }

@Entity(tableName = "tobacco_logs")
data class TobaccoLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMillis: Long,
    val productType: String, // TobaccoProductType.name
    val trigger: String?, // TobaccoTrigger.name
    val note: String?,
)

@Dao
interface TobaccoDao {
    @Insert
    suspend fun insertLog(log: TobaccoLogEntity): Long

    @Query("DELETE FROM tobacco_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("SELECT * FROM tobacco_logs ORDER BY timestampEpochMillis DESC")
    fun observeLogs(): Flow<List<TobaccoLogEntity>>

    @Query("SELECT * FROM tobacco_logs WHERE timestampEpochMillis >= :sinceEpochMillis ORDER BY timestampEpochMillis DESC")
    fun observeLogsSince(sinceEpochMillis: Long): Flow<List<TobaccoLogEntity>>

    @Query("SELECT * FROM tobacco_logs ORDER BY timestampEpochMillis DESC LIMIT 20")
    fun observeRecentLogs(): Flow<List<TobaccoLogEntity>>
}

class TobaccoRepository(private val dao: TobaccoDao) {
    fun observeLogs(): Flow<List<TobaccoLogEntity>> = dao.observeLogs()
    fun observeLogsSince(sinceEpochMillis: Long): Flow<List<TobaccoLogEntity>> = dao.observeLogsSince(sinceEpochMillis)
    fun observeRecentLogs(): Flow<List<TobaccoLogEntity>> = dao.observeRecentLogs()

    suspend fun logPuff(
        productType: TobaccoProductType,
        trigger: TobaccoTrigger?,
        note: String? = null,
        timestampEpochMillis: Long = System.currentTimeMillis(),
    ) {
        dao.insertLog(
            TobaccoLogEntity(
                timestampEpochMillis = timestampEpochMillis,
                productType = productType.name,
                trigger = trigger?.name,
                note = note,
            )
        )
    }

    suspend fun deleteLog(id: Long) = dao.deleteLog(id)
}
