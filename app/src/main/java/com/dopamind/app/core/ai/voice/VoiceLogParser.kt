package com.dopamind.app.core.ai.voice

/** Pure Kotlin — no Android dependency. Testable and portable to a future KMP target. */
enum class ParsedCategory { CANNABIS, TOBACCO, ALCOHOL }

data class ParsedLogItem(
    val category: ParsedCategory,
    val quantity: Int,
    val rawPhrase: String,
)

data class VoiceParseResult(
    val items: List<ParsedLogItem>,
    val transcript: String,
)

/**
 * Rule-based Italian/English NLU for hands-free logging: no cloud call, no
 * trained model — a keyword + number-word matcher over the on-device speech
 * transcript. This is the honest, offline-only scope of "voice-to-log" for
 * the MVP; a phrase it can't confidently parse is simply left unparsed and
 * shown to the user to confirm manually rather than guessed at.
 */
object VoiceLogParser {

    private val numberWords: Map<String, Int> = mapOf(
        "un" to 1, "uno" to 1, "una" to 1, "one" to 1, "a" to 1, "an" to 1,
        "due" to 2, "two" to 2,
        "tre" to 3, "three" to 3,
        "quattro" to 4, "four" to 4,
        "cinque" to 5, "five" to 5,
        "sei" to 6, "six" to 6,
        "sette" to 7, "seven" to 7,
        "otto" to 8, "eight" to 8,
        "nove" to 9, "nine" to 9,
        "dieci" to 10, "ten" to 10,
    )

    private val categoryKeywords: Map<ParsedCategory, List<String>> = mapOf(
        ParsedCategory.ALCOHOL to listOf(
            "birra", "birre", "beer", "beers", "vino", "wine", "cocktail", "cocktails",
            "shot", "shots", "drink", "drinks", "bicchiere", "bicchieri",
        ),
        ParsedCategory.TOBACCO to listOf(
            "sigaretta", "sigarette", "cigarette", "cigarettes", "iqos", "stick", "sticks",
            "tiro", "tiri", "puff", "puffs", "canna d'iqos",
        ),
        ParsedCategory.CANNABIS to listOf(
            "canna", "canne", "joint", "joints", "spinello", "spinelli", "erba", "weed", "bong",
        ),
    )

    private val numberPattern = "(\\d+|${numberWords.keys.joinToString("|")})".toRegex()

    fun parse(transcript: String): VoiceParseResult {
        val clauses = transcript
            .lowercase()
            .split(Regex("[,.]|\\be\\b|\\band\\b"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val items = clauses.mapNotNull { clause -> parseClause(clause) }
        return VoiceParseResult(items = items, transcript = transcript)
    }

    private fun parseClause(clause: String): ParsedLogItem? {
        val category = categoryKeywords.entries.firstOrNull { (_, keywords) ->
            keywords.any { clause.contains(it) }
        }?.key ?: return null

        val numberMatch = numberPattern.find(clause)?.value
        val quantity = numberMatch?.let { it.toIntOrNull() ?: numberWords[it] } ?: 1

        return ParsedLogItem(category = category, quantity = quantity, rawPhrase = clause)
    }
}
