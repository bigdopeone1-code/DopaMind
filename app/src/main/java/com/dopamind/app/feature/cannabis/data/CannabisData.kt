package com.dopamind.app.feature.cannabis.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class ConsumptionMethod { JOINT, VAPE, EDIBLE, TINCTURE, BONG, OTHER }

@Entity(tableName = "cannabis_logs")
data class CannabisLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMillis: Long,
    val strainName: String,
    val method: String, // ConsumptionMethod.name
    val thcPercent: Float?,
    val cbdPercent: Float?,
    val quantityGrams: Float?,
    val moodBefore: Int,
    val moodAfter: Int?,
    val note: String?,
)

@Entity(tableName = "cannabis_tbreaks")
data class TBreakEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMillis: Long,
    val targetDays: Int,
    val active: Boolean,
    val endedEpochMillis: Long?,
)

@Dao
interface CannabisDao {
    @Insert
    suspend fun insertLog(log: CannabisLogEntity): Long

    @Query("DELETE FROM cannabis_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("SELECT * FROM cannabis_logs ORDER BY timestampEpochMillis DESC")
    fun observeLogs(): Flow<List<CannabisLogEntity>>

    @Query("SELECT * FROM cannabis_logs WHERE timestampEpochMillis >= :sinceEpochMillis ORDER BY timestampEpochMillis DESC")
    fun observeLogsSince(sinceEpochMillis: Long): Flow<List<CannabisLogEntity>>

    @Query("SELECT * FROM cannabis_logs ORDER BY timestampEpochMillis DESC LIMIT 1")
    suspend fun lastLog(): CannabisLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTBreak(tBreak: TBreakEntity): Long

    @Update
    suspend fun updateTBreak(tBreak: TBreakEntity)

    @Query("SELECT * FROM cannabis_tbreaks WHERE active = 1 LIMIT 1")
    fun observeActiveTBreak(): Flow<TBreakEntity?>
}

class CannabisRepository(private val dao: CannabisDao) {
    fun observeLogs(): Flow<List<CannabisLogEntity>> = dao.observeLogs()
    fun observeLogsSince(sinceEpochMillis: Long): Flow<List<CannabisLogEntity>> = dao.observeLogsSince(sinceEpochMillis)
    fun observeActiveTBreak(): Flow<TBreakEntity?> = dao.observeActiveTBreak()

    suspend fun logConsumption(
        strainName: String,
        method: ConsumptionMethod,
        thcPercent: Float?,
        cbdPercent: Float?,
        quantityGrams: Float?,
        moodBefore: Int,
        moodAfter: Int?,
        note: String?,
        timestampEpochMillis: Long = System.currentTimeMillis(),
    ) {
        dao.insertLog(
            CannabisLogEntity(
                timestampEpochMillis = timestampEpochMillis,
                strainName = strainName,
                method = method.name,
                thcPercent = thcPercent,
                cbdPercent = cbdPercent,
                quantityGrams = quantityGrams,
                moodBefore = moodBefore,
                moodAfter = moodAfter,
                note = note,
            )
        )
    }

    suspend fun deleteLog(id: Long) = dao.deleteLog(id)

    suspend fun startTBreak(targetDays: Int, startEpochMillis: Long = System.currentTimeMillis()) {
        dao.upsertTBreak(
            TBreakEntity(
                startEpochMillis = startEpochMillis,
                targetDays = targetDays,
                active = true,
                endedEpochMillis = null,
            )
        )
    }

    suspend fun endTBreak(tBreak: TBreakEntity, endedEpochMillis: Long = System.currentTimeMillis()) {
        dao.updateTBreak(tBreak.copy(active = false, endedEpochMillis = endedEpochMillis))
    }
}
