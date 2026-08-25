package com.dopamind.app.core.habit

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.dopamind.app.R
import kotlinx.coroutines.flow.Flow

/** What preceded the event. Optional — captured only when the user offers it. */
enum class EventTrigger(val labelRes: Int) {
    STRESS(R.string.trigger_stress),
    BOREDOM(R.string.trigger_boredom),
    SOCIAL(R.string.trigger_social),
    ROUTINE(R.string.trigger_routine),
    CELEBRATION(R.string.trigger_celebration),
    FATIGUE(R.string.trigger_fatigue),
    LOW_MOOD(R.string.trigger_low_mood),
    OTHER(R.string.trigger_other),
}

/** Where the event happened. Optional. */
enum class EventContext(val labelRes: Int) {
    HOME(R.string.context_home),
    WORK(R.string.context_work),
    OUT(R.string.context_out),
    COMMUTE(R.string.context_commute),
    SOCIAL_EVENT(R.string.context_social_event),
    OTHER(R.string.context_other),
}

/**
 * The single logging backbone for every tracked behaviour.
 *
 * Before this, each behaviour had its own Room table, which only works when
 * the set of behaviours is fixed at build time. The product now lets users
 * choose their own categories during onboarding, so the store has to be
 * generic: one row per event, category as data rather than as a table name.
 *
 * The per-module tables (drinks, cannabis, tobacco…) still exist and still own
 * the domain-specific fields their specialist calculators need — ABV and volume
 * for the BAC math, THC mg for edibles dosing. Those are *detail* stores now;
 * this is the one the Daily Log, DopaScore and Body Status read.
 *
 * Only [category], [amount] and [timestampEpochMillis] are required. Everything
 * else is optional by design: logging has to stay a few seconds of work, so the
 * Add Event sheet never blocks on the enrichment fields.
 */
@Entity(
    tableName = "behavior_events",
    indices = [Index(value = ["timestampEpochMillis"]), Index(value = ["category"])],
)
data class BehaviorEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // HabitCategory.name
    /** Count of units, or minutes, per the category's [HabitCategory.measurement]. */
    val amount: Float,
    val timestampEpochMillis: Long,
    /** 1..5, how intense/strong it felt. Null when not offered. */
    val intensity: Int? = null,
    /** 1..5 mood at the moment of logging. Null when not offered. */
    val mood: Int? = null,
    /** 1..5 stress at the moment of logging. Null when not offered. */
    val stress: Int? = null,
    val trigger: String? = null, // EventTrigger.name
    val context: String? = null, // EventContext.name
    val note: String? = null,
)

/** Domain view of a stored event, with the string columns resolved back to enums. */
data class BehaviorEvent(
    val id: Long,
    val category: HabitCategory,
    val amount: Float,
    val timestampEpochMillis: Long,
    val intensity: Int?,
    val mood: Int?,
    val stress: Int?,
    val trigger: EventTrigger?,
    val context: EventContext?,
    val note: String?,
)

fun BehaviorEventEntity.toDomainOrNull(): BehaviorEvent? {
    val resolvedCategory = HabitCategory.fromNameOrNull(category) ?: return null
    return BehaviorEvent(
        id = id,
        category = resolvedCategory,
        amount = amount,
        timestampEpochMillis = timestampEpochMillis,
        intensity = intensity,
        mood = mood,
        stress = stress,
        trigger = trigger?.let { name -> EventTrigger.entries.firstOrNull { it.name == name } },
        context = context?.let { name -> EventContext.entries.firstOrNull { it.name == name } },
        note = note,
    )
}

@Dao
interface BehaviorEventDao {
    @Insert
    suspend fun insert(entity: BehaviorEventEntity): Long

    @Delete
    suspend fun delete(entity: BehaviorEventEntity)

    @Query("DELETE FROM behavior_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM behavior_events ORDER BY timestampEpochMillis DESC")
    fun observeAll(): Flow<List<BehaviorEventEntity>>

    @Query("SELECT * FROM behavior_events WHERE timestampEpochMillis >= :sinceEpochMillis ORDER BY timestampEpochMillis DESC")
    fun observeSince(sinceEpochMillis: Long): Flow<List<BehaviorEventEntity>>

    @Query(
        "SELECT * FROM behavior_events WHERE timestampEpochMillis >= :startEpochMillis " +
            "AND timestampEpochMillis < :endEpochMillis ORDER BY timestampEpochMillis DESC"
    )
    fun observeBetween(startEpochMillis: Long, endEpochMillis: Long): Flow<List<BehaviorEventEntity>>

    @Query("SELECT * FROM behavior_events WHERE category = :category ORDER BY timestampEpochMillis DESC LIMIT :limit")
    suspend fun recentForCategory(category: String, limit: Int): List<BehaviorEventEntity>
}

class BehaviorEventRepository(private val dao: BehaviorEventDao) {

    fun observeAll(): Flow<List<BehaviorEventEntity>> = dao.observeAll()

    fun observeSince(sinceEpochMillis: Long): Flow<List<BehaviorEventEntity>> = dao.observeSince(sinceEpochMillis)

    fun observeBetween(startEpochMillis: Long, endEpochMillis: Long): Flow<List<BehaviorEventEntity>> =
        dao.observeBetween(startEpochMillis, endEpochMillis)

    suspend fun log(
        category: HabitCategory,
        amount: Float,
        timestampEpochMillis: Long = System.currentTimeMillis(),
        intensity: Int? = null,
        mood: Int? = null,
        stress: Int? = null,
        trigger: EventTrigger? = null,
        context: EventContext? = null,
        note: String? = null,
    ): Long = dao.insert(
        BehaviorEventEntity(
            category = category.name,
            amount = amount,
            timestampEpochMillis = timestampEpochMillis,
            intensity = intensity,
            mood = mood,
            stress = stress,
            trigger = trigger?.name,
            context = context?.name,
            note = note?.takeIf { it.isNotBlank() },
        )
    )

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun recentForCategory(category: HabitCategory, limit: Int = 10): List<BehaviorEvent> =
        dao.recentForCategory(category.name, limit).mapNotNull { it.toDomainOrNull() }
}
