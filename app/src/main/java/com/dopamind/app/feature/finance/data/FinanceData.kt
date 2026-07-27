package com.dopamind.app.feature.finance.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class SpendCategory { CANNABIS, TOBACCO, ALCOHOL, OTHER }

@Entity(tableName = "spend_logs")
data class SpendLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMillis: Long,
    val category: String, // SpendCategory.name
    val amountEuros: Float,
    val quantity: Float?,
    val unit: String?,
    val note: String?,
)

@Entity(tableName = "budget_settings")
data class BudgetSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val monthlyLimitEuros: Float,
)

@Dao
interface FinanceDao {
    @Insert
    suspend fun insertSpend(entity: SpendLogEntity): Long

    @Query("DELETE FROM spend_logs WHERE id = :id")
    suspend fun deleteSpend(id: Long)

    @Query("SELECT * FROM spend_logs ORDER BY timestampEpochMillis DESC")
    fun observeSpends(): Flow<List<SpendLogEntity>>

    @Query("SELECT * FROM spend_logs WHERE timestampEpochMillis >= :sinceEpochMillis ORDER BY timestampEpochMillis DESC")
    fun observeSpendsSince(sinceEpochMillis: Long): Flow<List<SpendLogEntity>>

    @Query("SELECT * FROM spend_logs WHERE category = :category ORDER BY timestampEpochMillis DESC")
    fun observeSpendsForCategory(category: String): Flow<List<SpendLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(entity: BudgetSettingsEntity)

    @Query("SELECT * FROM budget_settings WHERE id = 1")
    fun observeBudget(): Flow<BudgetSettingsEntity?>
}

class FinanceRepository(private val dao: FinanceDao) {
    fun observeSpends(): Flow<List<SpendLogEntity>> = dao.observeSpends()
    fun observeSpendsSince(sinceEpochMillis: Long): Flow<List<SpendLogEntity>> = dao.observeSpendsSince(sinceEpochMillis)
    fun observeSpendsForCategory(category: SpendCategory): Flow<List<SpendLogEntity>> = dao.observeSpendsForCategory(category.name)
    fun observeBudget(): Flow<BudgetSettingsEntity?> = dao.observeBudget()

    suspend fun logSpend(
        category: SpendCategory,
        amountEuros: Float,
        quantity: Float?,
        unit: String?,
        note: String?,
        timestampEpochMillis: Long = System.currentTimeMillis(),
    ) {
        dao.insertSpend(
            SpendLogEntity(
                timestampEpochMillis = timestampEpochMillis,
                category = category.name,
                amountEuros = amountEuros,
                quantity = quantity,
                unit = unit,
                note = note,
            )
        )
    }

    suspend fun deleteSpend(id: Long) = dao.deleteSpend(id)

    suspend fun setMonthlyBudget(limitEuros: Float) = dao.upsertBudget(BudgetSettingsEntity(monthlyLimitEuros = limitEuros))
}
