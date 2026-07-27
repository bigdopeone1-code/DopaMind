package com.dopamind.app.feature.dopaminefocus.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class StackingType { DUTY, PLEASURE }

/** Captured right before a consumption log — feeds the craving-prediction AI module. */
@Entity(tableName = "why_prompts")
data class WhyPromptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMillis: Long,
    val triggerModule: String,
    val reason: String,
)

/** Health stacking: pairing a "duty" task with a "pleasure" reward. */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMillis: Long,
    val endEpochMillis: Long?,
    val label: String,
    val type: String, // StackingType.name
)

@Entity(tableName = "detox_sessions")
data class DetoxSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMillis: Long,
    val targetHours: Int,
    val endEpochMillis: Long?,
    val completed: Boolean,
)

@Dao
interface DopamineFocusDao {
    @Insert
    suspend fun insertWhyPrompt(entity: WhyPromptEntity): Long

    @Query("SELECT * FROM why_prompts ORDER BY timestampEpochMillis DESC")
    fun observeWhyPrompts(): Flow<List<WhyPromptEntity>>

    @Insert
    suspend fun insertFocusSession(entity: FocusSessionEntity): Long

    @Update
    suspend fun updateFocusSession(entity: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions ORDER BY startEpochMillis DESC")
    fun observeFocusSessions(): Flow<List<FocusSessionEntity>>

    @Insert
    suspend fun insertDetoxSession(entity: DetoxSessionEntity): Long

    @Update
    suspend fun updateDetoxSession(entity: DetoxSessionEntity)

    @Query("SELECT * FROM detox_sessions WHERE endEpochMillis IS NULL ORDER BY startEpochMillis DESC LIMIT 1")
    fun observeActiveDetoxSession(): Flow<DetoxSessionEntity?>

    @Query("SELECT * FROM detox_sessions ORDER BY startEpochMillis DESC")
    fun observeDetoxSessions(): Flow<List<DetoxSessionEntity>>
}

class DopamineFocusRepository(private val dao: DopamineFocusDao) {
    fun observeWhyPrompts(): Flow<List<WhyPromptEntity>> = dao.observeWhyPrompts()
    fun observeFocusSessions(): Flow<List<FocusSessionEntity>> = dao.observeFocusSessions()
    fun observeActiveDetoxSession(): Flow<DetoxSessionEntity?> = dao.observeActiveDetoxSession()
    fun observeDetoxSessions(): Flow<List<DetoxSessionEntity>> = dao.observeDetoxSessions()

    suspend fun logWhy(triggerModule: String, reason: String, timestampEpochMillis: Long = System.currentTimeMillis()) {
        dao.insertWhyPrompt(WhyPromptEntity(timestampEpochMillis = timestampEpochMillis, triggerModule = triggerModule, reason = reason))
    }

    suspend fun startStackingSession(label: String, type: StackingType, startEpochMillis: Long = System.currentTimeMillis()) {
        dao.insertFocusSession(FocusSessionEntity(startEpochMillis = startEpochMillis, endEpochMillis = null, label = label, type = type.name))
    }

    suspend fun completeStackingSession(session: FocusSessionEntity, endEpochMillis: Long = System.currentTimeMillis()) {
        dao.updateFocusSession(session.copy(endEpochMillis = endEpochMillis))
    }

    suspend fun startDetox(targetHours: Int, startEpochMillis: Long = System.currentTimeMillis()) {
        dao.insertDetoxSession(DetoxSessionEntity(startEpochMillis = startEpochMillis, targetHours = targetHours, endEpochMillis = null, completed = false))
    }

    suspend fun endDetox(session: DetoxSessionEntity, completed: Boolean, endEpochMillis: Long = System.currentTimeMillis()) {
        dao.updateDetoxSession(session.copy(endEpochMillis = endEpochMillis, completed = completed))
    }
}
