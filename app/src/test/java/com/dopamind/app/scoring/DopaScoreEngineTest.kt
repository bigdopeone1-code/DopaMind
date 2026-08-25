package com.dopamind.app.scoring

import com.dopamind.app.core.habit.HabitCategory
import com.dopamind.app.core.scoring.DayAggregate
import com.dopamind.app.core.scoring.DopaScoreEngine
import com.dopamind.app.core.scoring.ScoreBand
import com.dopamind.app.core.scoring.ScoreDimension
import com.dopamind.app.core.scoring.ScoreInput
import com.dopamind.app.core.scoring.ScoringConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scoring layer is pure Kotlin precisely so it can be covered like this —
 * no emulator, no Room, no Compose. These are the invariants the product relies
 * on; if one of them breaks, the number on the Home screen is lying.
 */
class DopaScoreEngineTest {

    private val today = 20_000L
    private val engine = DopaScoreEngine()
    private val config = ScoringConfig.DEFAULT

    private fun days(count: Int, build: (Long) -> DayAggregate): List<DayAggregate> =
        (0 until count).map { offset -> build(today - (count - 1 - offset)) }

    private fun emptyDay(day: Long) = DayAggregate(
        epochDay = day,
        loadByCategory = emptyMap(),
        sleepHours = null,
        sleepQuality = null,
        moodIndex = null,
        stressIndex = null,
    )

    @Test
    fun `no data at all reports zero coverage rather than a zero score`() {
        val input = ScoreInput(today, days(7) { emptyDay(it) }, trackedCategories = emptyList())

        val score = engine.evaluate(input)

        assertEquals(0f, score.coverage, 0.001f)
        assertTrue("no dimension should be marked available", score.availableDimensions.isEmpty())
    }

    @Test
    fun `dimensions without data are excluded, not scored zero`() {
        // Sleep is the only signal present. Habit Load is available too (the user
        // tracks a category and simply logged nothing), but Energy and Stress
        // have nothing behind them and must not drag the score down.
        val input = ScoreInput(
            todayEpochDay = today,
            days = days(7) { emptyDay(it).copy(sleepHours = 8f) },
            trackedCategories = listOf(HabitCategory.NICOTINE),
        )

        val score = engine.evaluate(input)

        val energy = score.dimensions.first { it.dimension == ScoreDimension.ENERGY }
        val stress = score.dimensions.first { it.dimension == ScoreDimension.STRESS }
        assertFalse("energy has no data", energy.available)
        assertFalse("stress has no data", stress.available)
        assertTrue("coverage should be partial", score.coverage > 0f && score.coverage < 1f)
        assertTrue("a full night's sleep with no load should score high", score.value >= 70)
    }

    @Test
    fun `heavier logged load lowers the score`() {
        fun scoreForLoad(load: Float): Int {
            val input = ScoreInput(
                todayEpochDay = today,
                days = days(7) { emptyDay(it).copy(loadByCategory = mapOf(HabitCategory.NICOTINE to load)) },
                trackedCategories = listOf(HabitCategory.NICOTINE),
            )
            return engine.evaluate(input).value
        }

        val light = scoreForLoad(0.1f)
        val heavy = scoreForLoad(1.0f)

        assertTrue("more load must score lower ($heavy should be < $light)", heavy < light)
    }

    @Test
    fun `recent days weigh more than older ones`() {
        // Exactly two loaded days in both cases, shifted to either end of the
        // window. Keeping the *count* of loaded days equal is what isolates
        // recency: vary it and the light-day term in Recovery moves too, and the
        // comparison stops being about recency at all.
        fun scoreWithLoadOn(daysAgo: Set<Long>): Int {
            val list = days(7) { day ->
                val load = if ((today - day) in daysAgo) 1.0f else 0f
                emptyDay(day).copy(loadByCategory = mapOf(HabitCategory.ALCOHOL to load))
            }
            return engine.evaluate(
                ScoreInput(today, list, trackedCategories = listOf(HabitCategory.ALCOHOL))
            ).value
        }

        val recent = scoreWithLoadOn(setOf(0L, 1L))
        val older = scoreWithLoadOn(setOf(5L, 6L))

        assertTrue(
            "the same load, more recently, should score lower (recent=$recent older=$older)",
            recent < older,
        )
    }

    @Test
    fun `score always lands inside 0 to 100 even under extreme load`() {
        val input = ScoreInput(
            todayEpochDay = today,
            days = days(7) { emptyDay(it).copy(loadByCategory = HabitCategory.entries.associateWith { 1f }) },
            trackedCategories = HabitCategory.entries.toList(),
        )

        val score = engine.evaluate(input)

        assertTrue("score in range: ${score.value}", score.value in 0..100)
        assertEquals(ScoreBand.UNDER_LOAD, score.band)
    }

    @Test
    fun `band thresholds map to the configured boundaries`() {
        assertEquals(ScoreBand.STRONG, config.bandFor(85))
        assertEquals(ScoreBand.GOOD, config.bandFor(70))
        assertEquals(ScoreBand.GOOD, config.bandFor(84))
        assertEquals(ScoreBand.STEADY, config.bandFor(50))
        assertEquals(ScoreBand.UNDER_LOAD, config.bandFor(49))
        assertEquals(ScoreBand.UNDER_LOAD, config.bandFor(0))
    }

    @Test
    fun `weakest and strongest dimensions are reported for the insight card`() {
        val input = ScoreInput(
            todayEpochDay = today,
            days = days(7) {
                emptyDay(it).copy(
                    sleepHours = 8f, // full marks
                    moodIndex = 1, // floor of the 1..5 scale
                )
            },
            trackedCategories = listOf(HabitCategory.CAFFEINE),
        )

        val score = engine.evaluate(input)

        assertNotNull(score.weakestDimension)
        assertEquals(ScoreDimension.ENERGY, score.weakestDimension?.dimension)
        assertTrue("strongest should beat weakest", score.strongestDimension!!.value > score.weakestDimension!!.value)
    }
}
