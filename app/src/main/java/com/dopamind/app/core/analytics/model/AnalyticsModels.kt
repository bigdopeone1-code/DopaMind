package com.dopamind.app.core.analytics.model

/**
 * One calendar day of cross-module activity, the common unit every
 * core/analytics calculator operates on. Building this is the only place
 * that needs to know about every feature module's raw log shape.
 */
data class DailyMetrics(
    val epochDay: Long,
    val cannabisUses: Int,
    val tobaccoUses: Int,
    val alcoholStandardDrinks: Int,
    val alcoholGramsPure: Double,
    val libidoEvents: Int,
    val sleepHours: Float?,
    val sleepQuality: Int?, // 1..5
    val moodEnergyIndex: Int?, // 0..4, from the Daily Vibe Check-in
    val spendEuros: Float,
)

enum class Trend { RISING, STABLE, FALLING }

data class DopamineDebtResult(
    val score: Int, // 0..100, higher = more accumulated high-stimulation load
    val trend: Trend,
    val topContributors: List<DebtContributor>,
)

enum class DebtContributor { CANNABIS, TOBACCO, ALCOHOL, LIBIDO, POOR_SLEEP }

enum class HangoverRiskLevel { LOW, MODERATE, HIGH, SEVERE }

enum class HangoverTip { DRINK_WATER, EAT_BEFORE_SLEEP, ELECTROLYTES, SLEEP_EARLY, SLOW_DOWN, STOP_DRINKING }

data class HangoverRiskResult(
    val riskScore: Int, // 0..100
    val level: HangoverRiskLevel,
    val tips: List<HangoverTip>,
)

enum class CorrelationStrength { NONE, WEAK, MODERATE, STRONG }
enum class CorrelationDirection { POSITIVE, NEGATIVE }

data class SleepCorrelation(
    val contributor: DebtContributor,
    val coefficient: Double, // Pearson r, -1..1
    val strength: CorrelationStrength,
    val direction: CorrelationDirection,
)
