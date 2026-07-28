package com.dopamind.app.core.ai.vision

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class DrinkLabelScanResult(
    val abvPercent: Float?,
    val volumeMl: Int?,
    val rawText: String,
)

/**
 * On-device OCR (ML Kit Text Recognition, Latin script) over a photographed
 * drink label, with a regex pass to pull out ABV% and volume. Runs entirely
 * on-device — no image or text ever leaves the phone.
 */
object LabelScannerEngine {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // e.g. "5%", "5.0 % vol", "12,5%ABV"
    private val abvPattern = Regex("""(\d{1,2}[.,]?\d{0,2})\s*%\s*(vol|abv)?""", RegexOption.IGNORE_CASE)

    // e.g. "330 ml", "33cl", "0.75 l", "0,75L"
    private val volumeMlPattern = Regex("""(\d{2,4})\s*ml""", RegexOption.IGNORE_CASE)
    private val volumeClPattern = Regex("""(\d{1,3})\s*cl""", RegexOption.IGNORE_CASE)
    private val volumeLPattern = Regex("""(\d[.,]\d{1,2})\s*l\b""", RegexOption.IGNORE_CASE)

    suspend fun scan(image: InputImage): DrinkLabelScanResult {
        val result = recognizer.process(image).await()
        val text = result.text
        return DrinkLabelScanResult(
            abvPercent = extractAbv(text),
            volumeMl = extractVolumeMl(text),
            rawText = text,
        )
    }

    private fun extractAbv(text: String): Float? {
        val match = abvPattern.find(text) ?: return null
        return match.groupValues[1].replace(',', '.').toFloatOrNull()
    }

    private fun extractVolumeMl(text: String): Int? {
        volumeMlPattern.find(text)?.let { return it.groupValues[1].toIntOrNull() }
        volumeClPattern.find(text)?.let { return it.groupValues[1].toIntOrNull()?.times(10) }
        volumeLPattern.find(text)?.let {
            val liters = it.groupValues[1].replace(',', '.').toFloatOrNull() ?: return null
            return (liters * 1000).toInt()
        }
        return null
    }
}
