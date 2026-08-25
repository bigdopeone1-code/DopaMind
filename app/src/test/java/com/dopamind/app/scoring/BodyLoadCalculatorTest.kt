package com.dopamind.app.scoring

import com.dopamind.app.core.habit.BodySystem
import com.dopamind.app.core.habit.HabitCategory
import com.dopamind.app.core.scoring.BodyLoadCalculator
import com.dopamind.app.core.scoring.DayAggregate
import com.dopamind.app.core.scoring.ScoreInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyLoadCalculatorTest {

    private val today = 20_000L

    private fun inputWith(loads: Map<HabitCategory, Float>) = ScoreInput(
        todayEpochDay = today,
        days = (0 until 7).map { offset ->
            DayAggregate(
                epochDay = today - (6 - offset),
                loadByCategory = loads,
                sleepHours = null,
                sleepQuality = null,
                moodIndex = null,
                stressIndex = null,
            )
        },
        trackedCategories = loads.keys.toList(),
    )

    @Test
    fun `every system reports a reading`() {
        val statuses = BodyLoadCalculator.calculate(inputWith(emptyMap()))
        assertEquals(BodySystem.entries.size, statuses.size)
        assertEquals(BodySystem.entries.toSet(), statuses.map { it.system }.toSet())
    }

    @Test
    fun `nothing logged reads as full`() {
        BodyLoadCalculator.calculate(inputWith(emptyMap())).forEach {
            assertEquals("${it.system} with no load", 100, it.value)
        }
    }

    @Test
    fun `load lands on the systems the category is weighted towards`() {
        // Nicotine carries a full weight on lungs and none on the liver.
        val statuses = BodyLoadCalculator.calculate(inputWith(mapOf(HabitCategory.NICOTINE to 1f)))
        val lungs = statuses.first { it.system == BodySystem.LUNGS }
        val liver = statuses.first { it.system == BodySystem.LIVER }

        assertTrue("lungs should drop: ${lungs.value}", lungs.value < 100)
        assertEquals("liver is untouched by nicotine", 100, liver.value)
        assertEquals(HabitCategory.NICOTINE, lungs.topContributor)
    }

    @Test
    fun `top contributor is the category pushing hardest on that system`() {
        val statuses = BodyLoadCalculator.calculate(
            inputWith(
                mapOf(
                    HabitCategory.ALCOHOL to 1f, // liver weight 1.0
                    HabitCategory.OVEREATING to 0.2f, // liver weight 0.7
                )
            )
        )
        val liver = statuses.first { it.system == BodySystem.LIVER }
        assertEquals(HabitCategory.ALCOHOL, liver.topContributor)
    }

    @Test
    fun `readings stay inside 0 to 100 under saturating load`() {
        val statuses = BodyLoadCalculator.calculate(
            inputWith(HabitCategory.entries.associateWith { 1f })
        )
        statuses.forEach { assertTrue("${it.system}=${it.value}", it.value in 0..100) }
    }
}
