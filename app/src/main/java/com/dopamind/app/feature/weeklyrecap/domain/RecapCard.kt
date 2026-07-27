package com.dopamind.app.feature.weeklyrecap.domain

import com.dopamind.app.core.analytics.model.Trend

/**
 * Pure Kotlin — no Android dependency. Carries values only; the UI layer
 * formats each card's copy from strings.xml so all user-facing text stays
 * externalized and localizable.
 */
enum class RecapCardType {
    TOTAL_SPEND,
    ALCOHOL_SPEND,
    SMOKE_FREE_DAYS,
    CANNABIS_SESSIONS,
    SLEEP_AVERAGE,
    DOPAMINE_DEBT_TREND,
    MOOD_AVERAGE,
    NO_DATA,
}

data class RecapCard(
    val type: RecapCardType,
    val primaryValue: Float = 0f,
    val secondaryLabel: String? = null,
    val trend: Trend? = null,
)
