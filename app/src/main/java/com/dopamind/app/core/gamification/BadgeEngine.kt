package com.dopamind.app.core.gamification

import com.dopamind.app.feature.alcohol.data.AlcoholRepository
import com.dopamind.app.feature.cannabis.data.CannabisRepository
import com.dopamind.app.feature.dailyvibe.data.DailyVibeRepository
import com.dopamind.app.feature.dopaminefocus.data.DopamineFocusRepository
import com.dopamind.app.feature.finance.data.FinanceRepository
import com.dopamind.app.feature.libido.data.LibidoRepository
import com.dopamind.app.feature.tobacco.data.TobaccoRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

/** A badge newly unlocked by [BadgeEngine.evaluate], to surface as a celebratory reveal. */
data class NewlyUnlockedBadge(val badgeId: BadgeId)

/**
 * Evaluates every badge's unlock condition against current app state and
 * persists any newly-earned ones. Call after check-in, after a module log,
 * or when opening the dashboard — it's idempotent and cheap.
 */
class BadgeEngine(
    private val gamificationRepository: GamificationRepository,
    private val dailyVibeRepository: DailyVibeRepository,
    private val cannabisRepository: CannabisRepository,
    private val tobaccoRepository: TobaccoRepository,
    private val alcoholRepository: AlcoholRepository,
    private val libidoRepository: LibidoRepository,
    private val dopamineFocusRepository: DopamineFocusRepository,
    private val financeRepository: FinanceRepository,
) {
    suspend fun evaluate(nowEpochMillis: Long = System.currentTimeMillis()): List<NewlyUnlockedBadge> {
        val todayEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val newlyUnlocked = mutableListOf<NewlyUnlockedBadge>()

        suspend fun tryUnlock(id: BadgeId, condition: suspend () -> Boolean) {
            if (condition() && gamificationRepository.unlock(id.name, nowEpochMillis)) {
                newlyUnlocked += NewlyUnlockedBadge(id)
            }
        }

        tryUnlock(BadgeId.CHECKIN_STREAK_7) { dailyVibeRepository.currentStreak(todayEpochDay) >= 7 }
        tryUnlock(BadgeId.CHECKIN_STREAK_30) { dailyVibeRepository.currentStreak(todayEpochDay) >= 30 }

        tryUnlock(BadgeId.TBREAK_HERO_7) {
            val tBreak = cannabisRepository.observeActiveTBreak().first()
            val elapsedDays = tBreak?.let { (nowEpochMillis - it.startEpochMillis) / 86_400_000L } ?: 0L
            elapsedDays >= 7
        }

        tryUnlock(BadgeId.SMOKE_FREE_3) {
            val last = tobaccoRepository.observeLogs().first().maxByOrNull { it.timestampEpochMillis }
            val daysSince = last?.let { (nowEpochMillis - it.timestampEpochMillis) / 86_400_000L } ?: Long.MAX_VALUE
            daysSince >= 3
        }

        tryUnlock(BadgeId.LIVER_FRIEND_7) {
            val last = alcoholRepository.observeLogs().first().maxByOrNull { it.timestampEpochMillis }
            val daysSince = last?.let { (nowEpochMillis - it.timestampEpochMillis) / 86_400_000L } ?: Long.MAX_VALUE
            daysSince >= 7
        }

        tryUnlock(BadgeId.MONK_MODE_7) {
            val last = libidoRepository.observeLogs().first().maxByOrNull { it.timestampEpochMillis }
            val daysSince = last?.let { (nowEpochMillis - it.timestampEpochMillis) / 86_400_000L } ?: Long.MAX_VALUE
            daysSince >= 7
        }

        tryUnlock(BadgeId.DETOX_MASTER_3) {
            dopamineFocusRepository.observeDetoxSessions().first().count { it.completed } >= 3
        }

        tryUnlock(BadgeId.BUDGET_BOSS) {
            val budget = financeRepository.observeBudget().first() ?: return@tryUnlock false
            val monthStartEpochDay = LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1).toEpochDay()
            val spentThisMonth = financeRepository.observeSpendsSince(monthStartEpochDay * 86_400_000L).first()
                .sumOf { it.amountEuros.toDouble() }
            spentThisMonth <= budget.monthlyLimitEuros
        }

        return newlyUnlocked
    }
}
