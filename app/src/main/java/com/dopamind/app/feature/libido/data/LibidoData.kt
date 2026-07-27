package com.dopamind.app.feature.libido.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class LibidoActivityType { SOLO, PARTNER }

@Entity(tableName = "libido_logs")
data class LibidoLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMillis: Long,
    val activityType: String, // LibidoActivityType.name
    val moodBefore: Int,
    val moodAfter: Int?,
    val note: String?,
)

@Dao
interface LibidoDao {
    @Insert
    suspend fun insertLog(log: LibidoLogEntity): Long

    @Query("DELETE FROM libido_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("SELECT * FROM libido_logs ORDER BY timestampEpochMillis DESC")
    fun observeLogs(): Flow<List<LibidoLogEntity>>

    @Query("SELECT * FROM libido_logs ORDER BY timestampEpochMillis DESC LIMIT 1")
    fun observeLastLog(): Flow<LibidoLogEntity?>
}

class LibidoRepository(private val dao: LibidoDao) {
    fun observeLogs(): Flow<List<LibidoLogEntity>> = dao.observeLogs()
    fun observeLastLog(): Flow<LibidoLogEntity?> = dao.observeLastLog()

    suspend fun logActivity(
        activityType: LibidoActivityType,
        moodBefore: Int,
        moodAfter: Int?,
        note: String?,
        timestampEpochMillis: Long = System.currentTimeMillis(),
    ) {
        dao.insertLog(
            LibidoLogEntity(
                timestampEpochMillis = timestampEpochMillis,
                activityType = activityType.name,
                moodBefore = moodBefore,
                moodAfter = moodAfter,
                note = note,
            )
        )
    }

    suspend fun deleteLog(id: Long) = dao.deleteLog(id)
}

/** Pure Kotlin streak math shared by the "monk mode" UI. */
object StreakCalculator {
    fun daysSince(lastEventEpochMillis: Long?, nowEpochMillis: Long): Int {
        if (lastEventEpochMillis == null) return 0
        val diff = nowEpochMillis - lastEventEpochMillis
        return (diff / 86_400_000L).toInt().coerceAtLeast(0)
    }
}
