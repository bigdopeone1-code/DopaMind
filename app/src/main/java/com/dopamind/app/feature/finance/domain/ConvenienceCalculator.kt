package com.dopamind.app.feature.finance.domain

/** Pure Kotlin — no Android dependency. */
data class PricePoint(val amountEuros: Float, val quantity: Float, val timestampEpochMillis: Long)

data class ConvenienceResult(
    val pricePerUnit: Float,
    val averagePricePerUnit: Float,
    val cheaperThanAverageByPercent: Float,
)

object ConvenienceCalculator {

    fun pricePerUnit(amountEuros: Float, quantity: Float): Float {
        if (quantity <= 0f) return 0f
        return amountEuros / quantity
    }

    /** Compares a new purchase against the user's own price history for the same category/unit. */
    fun analyze(newPoint: PricePoint, history: List<PricePoint>): ConvenienceResult {
        val newPricePerUnit = pricePerUnit(newPoint.amountEuros, newPoint.quantity)
        val allPoints = history + newPoint
        val averagePricePerUnit = allPoints
            .map { pricePerUnit(it.amountEuros, it.quantity) }
            .filter { it > 0f }
            .average()
            .toFloat()
            .takeIf { !it.isNaN() } ?: newPricePerUnit

        val diffPercent = if (averagePricePerUnit == 0f) {
            0f
        } else {
            ((averagePricePerUnit - newPricePerUnit) / averagePricePerUnit) * 100f
        }

        return ConvenienceResult(
            pricePerUnit = newPricePerUnit,
            averagePricePerUnit = averagePricePerUnit,
            cheaperThanAverageByPercent = diffPercent,
        )
    }
}
