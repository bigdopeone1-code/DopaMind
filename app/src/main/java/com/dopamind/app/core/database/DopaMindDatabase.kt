package com.dopamind.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dopamind.app.core.gamification.BadgeUnlockEntity
import com.dopamind.app.core.gamification.GamificationDao
import com.dopamind.app.core.security.PassphraseProvider
import com.dopamind.app.feature.alcohol.data.AlcoholDao
import com.dopamind.app.feature.alcohol.data.DrinkLogEntity
import com.dopamind.app.feature.cannabis.data.CannabisDao
import com.dopamind.app.feature.cannabis.data.CannabisLogEntity
import com.dopamind.app.feature.cannabis.data.TBreakEntity
import com.dopamind.app.feature.dailyvibe.data.DailyVibeDao
import com.dopamind.app.feature.dailyvibe.data.DailyVibeEntity
import com.dopamind.app.feature.dopaminefocus.data.DetoxSessionEntity
import com.dopamind.app.feature.dopaminefocus.data.DopamineFocusDao
import com.dopamind.app.feature.dopaminefocus.data.FocusSessionEntity
import com.dopamind.app.feature.dopaminefocus.data.WhyPromptEntity
import com.dopamind.app.feature.finance.data.BudgetSettingsEntity
import com.dopamind.app.feature.finance.data.FinanceDao
import com.dopamind.app.feature.finance.data.SpendLogEntity
import com.dopamind.app.feature.libido.data.LibidoDao
import com.dopamind.app.feature.libido.data.LibidoLogEntity
import com.dopamind.app.feature.nutrition.data.FastingSessionEntity
import com.dopamind.app.feature.nutrition.data.FoodLogEntity
import com.dopamind.app.feature.nutrition.data.NutritionDao
import com.dopamind.app.feature.nutrition.data.WaterLogEntity
import com.dopamind.app.feature.nutrition.data.WeightLogEntity
import com.dopamind.app.feature.profile.data.ProfileDao
import com.dopamind.app.feature.profile.data.UserProfileEntity
import com.dopamind.app.feature.recovery.data.MunchiesLogEntity
import com.dopamind.app.feature.recovery.data.RecoveryDao
import com.dopamind.app.feature.recovery.data.SleepLogEntity
import com.dopamind.app.feature.recovery.data.SosSessionEntity
import com.dopamind.app.feature.tobacco.data.TobaccoDao
import com.dopamind.app.feature.tobacco.data.TobaccoLogEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        CannabisLogEntity::class,
        TBreakEntity::class,
        TobaccoLogEntity::class,
        DrinkLogEntity::class,
        LibidoLogEntity::class,
        WhyPromptEntity::class,
        FocusSessionEntity::class,
        DetoxSessionEntity::class,
        SleepLogEntity::class,
        MunchiesLogEntity::class,
        SosSessionEntity::class,
        SpendLogEntity::class,
        BudgetSettingsEntity::class,
        DailyVibeEntity::class,
        BadgeUnlockEntity::class,
        UserProfileEntity::class,
        FoodLogEntity::class,
        WeightLogEntity::class,
        WaterLogEntity::class,
        FastingSessionEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class DopaMindDatabase : RoomDatabase() {
    abstract fun cannabisDao(): CannabisDao
    abstract fun tobaccoDao(): TobaccoDao
    abstract fun alcoholDao(): AlcoholDao
    abstract fun libidoDao(): LibidoDao
    abstract fun dopamineFocusDao(): DopamineFocusDao
    abstract fun recoveryDao(): RecoveryDao
    abstract fun financeDao(): FinanceDao
    abstract fun dailyVibeDao(): DailyVibeDao
    abstract fun gamificationDao(): GamificationDao
    abstract fun profileDao(): ProfileDao
    abstract fun nutritionDao(): NutritionDao

    companion object {
        private const val DATABASE_NAME = "dopamind_encrypted.db"

        fun databaseFile(context: Context) = context.getDatabasePath(DATABASE_NAME)

        fun create(context: Context): DopaMindDatabase {
            // Loads SQLCipher's native library; required once before first use.
            SQLiteDatabase.loadLibs(context)

            val passphrase = PassphraseProvider.getOrCreateDatabasePassphrase(context)
            val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase))

            return Room.databaseBuilder(context, DopaMindDatabase::class.java, DATABASE_NAME)
                .openHelperFactory(factory)
                // Pre-release MVP data only — acceptable to recreate on schema bumps
                // rather than hand-writing migrations for every enum/column addition.
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
