package com.dopamind.app.core.analytics

import com.dopamind.app.core.analytics.model.CorrelationDirection
import com.dopamind.app.core.analytics.model.CorrelationStrength
import com.dopamind.app.core.analytics.model.DailyMetrics
import com.dopamind.app.core.analytics.model.DebtContributor
import com.dopamind.app.core.analytics.model.SleepCorrelation
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure Kotlin — no Android dependency.
 *
 * Computes a simple Pearson correlation between each module's daily usage
 * and that day's logged sleep quality, so the app can say things like
 * "your sleep quality drops on days you smoke" with an actual number behind it
 * instead of a vibe.
 */
object SleepCorrelationAnalyzer {

    private const val MIN_DATA_POINTS = 4

    fun analyze(days: List<DailyMetrics>): List<SleepCorrelation> {
        val withSleep = days.filter { it.sleepQuality != null }
        if (withSleep.size < MIN_DATA_POINTS) return emptyList()

        val qualities = withSleep.map { it.sleepQuality!!.toDouble() }

        return listOf(
            DebtContributor.CANNABIS to withSleep.map { it.cannabisUses.toDouble() },
            DebtContributor.TOBACCO to withSleep.map { it.tobaccoUses.toDouble() },
            DebtContributor.ALCOHOL to withSleep.map { it.alcoholStandardDrinks.toDouble() },
            DebtContributor.LIBIDO to withSleep.map { it.libidoEvents.toDouble() },
        ).mapNotNull { (contributor, usage) ->
            val r = pearson(usage, qualities) ?: return@mapNotNull null
            SleepCorrelation(
                contributor = contributor,
                coefficient = r,
                strength = strengthOf(r),
                direction = if (r >= 0) CorrelationDirection.POSITIVE else CorrelationDirection.NEGATIVE,
            )
        }
    }

    private fun strengthOf(r: Double): CorrelationStrength = when {
        abs(r) < 0.1 -> CorrelationStrength.NONE
        abs(r) < 0.3 -> CorrelationStrength.WEAK
        abs(r) < 0.5 -> CorrelationStrength.MODERATE
        else -> CorrelationStrength.STRONG
    }

    private fun pearson(x: List<Double>, y: List<Double>): Double? {
        if (x.size != y.size || x.size < MIN_DATA_POINTS) return null
        if (x.all { it == x.first() }) return null // no variance, correlation undefined

        val n = x.size
        val meanX = x.average()
        val meanY = y.average()

        var covariance = 0.0
        var varX = 0.0
        var varY = 0.0
        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            covariance += dx * dy
            varX += dx * dx
            varY += dy * dy
        }

        val denominator = sqrt(varX * varY)
        if (denominator == 0.0) return null
        return (covariance / denominator).coerceIn(-1.0, 1.0)
    }
}
