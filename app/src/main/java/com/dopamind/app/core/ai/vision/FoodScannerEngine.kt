package com.dopamind.app.core.ai.vision

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

data class FoodScanResult(
    val topLabel: String?,
    val junkScore: Int, // 0..100 heuristic, purely informational
    val allLabels: List<String>,
)

/**
 * On-device image labeling (ML Kit's generic labeler, not a custom-trained
 * food model — that would need a dataset and training infra out of MVP
 * scope). Maps the returned labels against a junk-food keyword list to get
 * a rough "how heavy is this on your stomach/sleep" score.
 */
object FoodScannerEngine {

    private val labeler by lazy { ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS) }

    private val junkKeywords = listOf(
        "pizza", "burger", "hamburger", "fries", "fast food", "fried food", "chips",
        "donut", "doughnut", "cookie", "candy", "chocolate", "ice cream", "cake",
        "soft drink", "kebab", "nachos", "popcorn",
    )
    private val lightKeywords = listOf(
        "salad", "vegetable", "fruit", "fish", "grilled", "soup", "yogurt", "rice",
    )

    suspend fun scan(image: InputImage): FoodScanResult {
        val labels = labeler.process(image).await()
        val labelTexts = labels.sortedByDescending { it.confidence }.map { it.text }

        val lowerLabels = labelTexts.map { it.lowercase() }
        val junkHits = lowerLabels.count { label -> junkKeywords.any { label.contains(it) } }
        val lightHits = lowerLabels.count { label -> lightKeywords.any { label.contains(it) } }

        val score = (50 + junkHits * 20 - lightHits * 20).coerceIn(0, 100)

        return FoodScanResult(
            topLabel = labelTexts.firstOrNull(),
            junkScore = score,
            allLabels = labelTexts,
        )
    }
}
