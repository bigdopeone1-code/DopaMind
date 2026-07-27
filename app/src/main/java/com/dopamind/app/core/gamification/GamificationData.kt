package com.dopamind.app.core.gamification

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "badge_unlocks")
data class BadgeUnlockEntity(
    @PrimaryKey val badgeId: String,
    val unlockedAtEpochMillis: Long,
)

@Dao
interface GamificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlock(entity: BadgeUnlockEntity): Long

    @Query("SELECT * FROM badge_unlocks ORDER BY unlockedAtEpochMillis DESC")
    fun observeUnlocks(): Flow<List<BadgeUnlockEntity>>

    @Query("SELECT badgeId FROM badge_unlocks")
    suspend fun unlockedBadgeIds(): List<String>
}

class GamificationRepository(private val dao: GamificationDao) {
    fun observeUnlocks(): Flow<List<BadgeUnlockEntity>> = dao.observeUnlocks()

    suspend fun unlockedBadgeIds(): Set<String> = dao.unlockedBadgeIds().toSet()

    /** Returns true if this call newly unlocked the badge (i.e. it wasn't already owned). */
    suspend fun unlock(badgeId: String, atEpochMillis: Long = System.currentTimeMillis()): Boolean {
        val alreadyUnlocked = dao.unlockedBadgeIds().contains(badgeId)
        if (alreadyUnlocked) return false
        dao.insertUnlock(BadgeUnlockEntity(badgeId = badgeId, unlockedAtEpochMillis = atEpochMillis))
        return true
    }
}
