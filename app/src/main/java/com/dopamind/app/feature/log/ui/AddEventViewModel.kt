package com.dopamind.app.feature.log.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopamind.app.core.habit.BehaviorEventRepository
import com.dopamind.app.core.habit.EventContext
import com.dopamind.app.core.habit.EventTrigger
import com.dopamind.app.core.habit.HabitCategory
import com.dopamind.app.feature.profile.data.ProfileRepository
import com.dopamind.app.feature.profile.data.trackedCategories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How long ago the event happened, offered as one-tap options instead of a picker. */
enum class WhenOption(val minutesAgo: Int) {
    NOW(0),
    FIFTEEN_MIN(15),
    ONE_HOUR(60),
    THREE_HOURS(180),
}

data class AddEventUiState(
    /** Null until the user picks one — the screen opens on the category step. */
    val category: HabitCategory? = null,
    val amount: Float = 0f,
    val whenOption: WhenOption = WhenOption.NOW,
    val intensity: Int? = null,
    val mood: Int? = null,
    val stress: Int? = null,
    val trigger: EventTrigger? = null,
    val context: EventContext? = null,
    val note: String = "",
    /** Categories the user tracks, shown first in the picker. */
    val trackedCategories: List<HabitCategory> = emptyList(),
    val showAllCategories: Boolean = false,
    val saved: Boolean = false,
) {
    /**
     * Category and a non-zero amount are all that is ever required. Everything
     * below the fold is enrichment — the product bet is that a log that takes
     * three taps gets written and a log that takes twelve does not.
     */
    val canSave: Boolean get() = category != null && amount > 0f
}

class AddEventViewModel(
    private val behaviorEventRepository: BehaviorEventRepository,
    private val profileRepository: ProfileRepository,
    presetCategoryName: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddEventUiState(category = presetCategoryName?.let { HabitCategory.fromNameOrNull(it) })
    )
    val uiState: StateFlow<AddEventUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val tracked = profileRepository.getProfileOnce()?.trackedCategories()
                ?: HabitCategory.DEFAULT_SELECTION
            _uiState.update { it.copy(trackedCategories = tracked) }
        }
    }

    fun selectCategory(category: HabitCategory) = _uiState.update {
        // Switching category resets the amount: the units differ (cigarettes vs
        // minutes), so carrying a number across would silently mean something else.
        it.copy(category = category, amount = 0f)
    }

    fun clearCategory() = _uiState.update { it.copy(category = null, amount = 0f) }
    fun toggleShowAllCategories() = _uiState.update { it.copy(showAllCategories = !it.showAllCategories) }

    fun setAmount(amount: Float) = _uiState.update { it.copy(amount = amount.coerceAtLeast(0f)) }
    fun addAmount(delta: Int) = _uiState.update { it.copy(amount = (it.amount + delta).coerceAtLeast(0f)) }

    fun setWhen(option: WhenOption) = _uiState.update { it.copy(whenOption = option) }

    // Tapping the selected value again clears it — every enrichment field stays
    // genuinely optional, including after a mis-tap.
    fun setIntensity(value: Int) = _uiState.update { it.copy(intensity = it.intensity.toggle(value)) }
    fun setMood(value: Int) = _uiState.update { it.copy(mood = it.mood.toggle(value)) }
    fun setStress(value: Int) = _uiState.update { it.copy(stress = it.stress.toggle(value)) }

    fun setTrigger(trigger: EventTrigger) = _uiState.update {
        it.copy(trigger = if (it.trigger == trigger) null else trigger)
    }

    fun setContext(context: EventContext) = _uiState.update {
        it.copy(context = if (it.context == context) null else context)
    }

    fun setNote(note: String) = _uiState.update { it.copy(note = note) }

    fun save() {
        val state = _uiState.value
        val category = state.category ?: return
        if (!state.canSave) return

        viewModelScope.launch {
            behaviorEventRepository.log(
                category = category,
                amount = state.amount,
                timestampEpochMillis = System.currentTimeMillis() - state.whenOption.minutesAgo * 60_000L,
                intensity = state.intensity,
                mood = state.mood,
                stress = state.stress,
                trigger = state.trigger,
                context = state.context,
                note = state.note,
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}

private fun Int?.toggle(value: Int): Int? = if (this == value) null else value
