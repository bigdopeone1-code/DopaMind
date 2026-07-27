package com.dopamind.app.core.ai.coach

import com.dopamind.app.feature.dailyvibe.data.DailyVibeRepository
import com.dopamind.app.feature.recovery.data.RecoveryRepository
import kotlinx.coroutines.flow.first

enum class BreathingPhase { INHALE, HOLD, EXHALE, REST }
data class BreathingStep(val phase: BreathingPhase, val durationSeconds: Int)

enum class ChillCoachMessage {
    WELCOME_FIRST_TIME,
    WELCOME_RETURNING,
    REASSURANCE_TEMPORARY,
    REASSURANCE_SAFE_PLACE,
    REASSURANCE_BREATHE,
    REASSURANCE_PASSES,
    GROUNDING_5_4_3_2_1,
    REASSURANCE_LOW_MOOD_CONTEXT,
}

data class ChillCoachSession(
    val openingMessage: ChillCoachMessage,
    val reassuranceSequence: List<ChillCoachMessage>,
    val breathingPattern: List<BreathingStep>,
)

/**
 * Rule-based (scripted decision tree), fully offline SOS companion for a
 * bad trip / paranoia moment — deliberately NOT a general-purpose chat
 * model. A fixed, reviewed script is the safer choice for this use case
 * than free-form generation, and it needs no network or cloud LLM.
 */
class ChillCoachEngine(
    private val recoveryRepository: RecoveryRepository,
    private val dailyVibeRepository: DailyVibeRepository,
) {
    suspend fun buildSession(nowEpochMillis: Long = System.currentTimeMillis()): ChillCoachSession {
        val priorSosCount = recoveryRepository.observeSosSessions().first().size
        val recentVibes = dailyVibeRepository.observeSince((nowEpochMillis / 86_400_000L) - 6).first()
        val lowMoodStretch = recentVibes.size >= 3 && recentVibes.takeLast(3).all { it.moodEnergyIndex <= 1 }

        val opening = if (priorSosCount == 0) ChillCoachMessage.WELCOME_FIRST_TIME else ChillCoachMessage.WELCOME_RETURNING

        val reassurance = buildList {
            add(ChillCoachMessage.REASSURANCE_TEMPORARY)
            add(ChillCoachMessage.REASSURANCE_SAFE_PLACE)
            add(ChillCoachMessage.REASSURANCE_BREATHE)
            add(ChillCoachMessage.GROUNDING_5_4_3_2_1)
            add(ChillCoachMessage.REASSURANCE_PASSES)
            if (lowMoodStretch) add(ChillCoachMessage.REASSURANCE_LOW_MOOD_CONTEXT)
        }

        // Box breathing — simple, well-known, no medical claims attached.
        val breathing = listOf(
            BreathingStep(BreathingPhase.INHALE, 4),
            BreathingStep(BreathingPhase.HOLD, 4),
            BreathingStep(BreathingPhase.EXHALE, 4),
            BreathingStep(BreathingPhase.REST, 4),
        )

        return ChillCoachSession(
            openingMessage = opening,
            reassuranceSequence = reassurance,
            breathingPattern = breathing,
        )
    }

    suspend fun recordSessionUsed(atEpochMillis: Long = System.currentTimeMillis()) {
        recoveryRepository.logSosUsage(atEpochMillis)
    }
}
