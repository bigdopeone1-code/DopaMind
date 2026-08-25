package com.dopamind.app.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopamind.app.R
import com.dopamind.app.core.habit.BehaviorEvent
import com.dopamind.app.core.habit.BehaviorEventRepository
import com.dopamind.app.core.habit.HabitCategory
import com.dopamind.app.core.habit.toDomainOrNull
import com.dopamind.app.core.scoring.BodyLoadCalculator
import com.dopamind.app.core.scoring.BodySystemStatus
import com.dopamind.app.core.scoring.DopaScore
import com.dopamind.app.core.scoring.DopaScoreEngine
import com.dopamind.app.core.scoring.ScoreInputBuilder
import com.dopamind.app.core.scoring.ScoringConfig
import com.dopamind.app.feature.profile.data.ProfileRepository
import com.dopamind.app.feature.profile.data.trackedCategories
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private const val MILLIS_PER_DAY = 86_400_000L

/** Today's running total for one tracked category. */
data class TodayCategoryTotal(
    val category: HabitCategory,
    val amount: Float,
    val eventCount: Int,
)

/** Time-of-day greeting. Resolved to copy in the UI layer. */
enum class GreetingSlot(val labelRes: Int) {
    MORNING(R.string.greeting_morning),
    AFTERNOON(R.string.greeting_afternoon),
    EVENING(R.string.greeting_evening),
}

data class HomeUiState(
    val loading: Boolean = true,
    val displayName: String = "",
    val greeting: GreetingSlot = GreetingSlot.MORNING,
    val score: DopaScore? = null,
    val bodySystems: List<BodySystemStatus> = emptyList(),
    val trackedCategories: List<HabitCategory> = emptyList(),
    val todayTotals: List<TodayCategoryTotal> = emptyList(),
    val todayEvents: List<BehaviorEvent> = emptyList(),
)

class HomeViewModel(
    private val behaviorEventRepository: BehaviorEventRepository,
    private val profileRepository: ProfileRepository,
    private val scoreInputBuilder: ScoreInputBuilder,
    private val dopaScoreEngine: DopaScoreEngine,
    private val config: ScoringConfig = ScoringConfig.DEFAULT,
) : ViewModel() {

    private fun todayEpochDay(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    private fun greetingSlot(): GreetingSlot = when (LocalTime.now().hour) {
        in 5..11 -> GreetingSlot.MORNING
        in 12..17 -> GreetingSlot.AFTERNOON
        else -> GreetingSlot.EVENING
    }

    /**
     * Recomputed whenever the profile or the event log changes. The score is
     * rebuilt from scratch on each emission rather than incrementally patched —
     * the window is a handful of days and the engine is pure arithmetic, so
     * correctness is worth more here than avoiding the recompute.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        profileRepository.observeProfile(),
        behaviorEventRepository.observeSince((todayEpochDay() - config.windowDays + 1) * MILLIS_PER_DAY),
    ) { profile, windowEvents -> profile to windowEvents }
        .map { (profile, windowEvents) ->
            val today = todayEpochDay()
            val todayStart = today * MILLIS_PER_DAY

            val todayEvents = windowEvents
                .filter { it.timestampEpochMillis >= todayStart }
                .mapNotNull { it.toDomainOrNull() }

            val tracked = profile?.trackedCategories() ?: HabitCategory.DEFAULT_SELECTION

            val totalsByCategory = todayEvents.groupBy { it.category }
            // Every tracked category gets a row, including the ones with nothing
            // logged today — a visible zero is the point, and it doubles as the
            // one-tap entry point for logging that category.
            val todayTotals = tracked.map { category ->
                val events = totalsByCategory[category].orEmpty()
                TodayCategoryTotal(
                    category = category,
                    amount = events.sumOf { it.amount.toDouble() }.toFloat(),
                    eventCount = events.size,
                )
            }

            val scoreInput = scoreInputBuilder.build(config)

            HomeUiState(
                loading = false,
                displayName = profile?.displayName.orEmpty(),
                greeting = greetingSlot(),
                score = dopaScoreEngine.evaluate(scoreInput),
                bodySystems = BodyLoadCalculator.calculate(scoreInput, config),
                trackedCategories = tracked,
                todayTotals = todayTotals,
                todayEvents = todayEvents,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun quickLog(category: HabitCategory, amount: Float) {
        viewModelScope.launch { behaviorEventRepository.log(category = category, amount = amount) }
    }

    fun undo(eventId: Long) {
        viewModelScope.launch { behaviorEventRepository.delete(eventId) }
    }
}
