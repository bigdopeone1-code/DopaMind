package com.dopamind.app.feature.dailyvibe.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopamind.app.core.gamification.BadgeEngine
import com.dopamind.app.core.gamification.BadgeId
import com.dopamind.app.feature.dailyvibe.data.DailyVibeRepository
import com.dopamind.app.feature.dailyvibe.domain.CheckInReaction
import com.dopamind.app.feature.dailyvibe.domain.CheckInReactionPicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

enum class ConsumedModuleChip { CANNABIS, TOBACCO, ALCOHOL, LIBIDO }

data class DailyVibeUiState(
    val moodEnergyIndex: Int = 2,
    val selectedModules: Set<ConsumedModuleChip> = emptySet(),
    val sleepHours: Float = 7f,
    val note: String = "",
    val submitting: Boolean = false,
    val result: DailyVibeResult? = null,
)

data class DailyVibeResult(
    val reaction: CheckInReaction,
    val newlyUnlockedBadges: List<BadgeId>,
    val streak: Int,
)

class DailyVibeCheckInViewModel(
    private val dailyVibeRepository: DailyVibeRepository,
    private val badgeEngine: BadgeEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyVibeUiState())
    val uiState: StateFlow<DailyVibeUiState> = _uiState.asStateFlow()

    fun onMoodChange(index: Int) = _uiState.update { it.copy(moodEnergyIndex = index) }
    fun onSleepHoursChange(hours: Float) = _uiState.update { it.copy(sleepHours = hours) }
    fun onNoteChange(note: String) = _uiState.update { it.copy(note = note) }

    fun onToggleModule(chip: ConsumedModuleChip) = _uiState.update { state ->
        val updated = if (chip in state.selectedModules) state.selectedModules - chip else state.selectedModules + chip
        state.copy(selectedModules = updated)
    }

    fun submit() {
        if (_uiState.value.submitting) return
        _uiState.update { it.copy(submitting = true) }

        viewModelScope.launch {
            val state = _uiState.value
            val now = Instant.now()
            val today = now.atZone(ZoneId.systemDefault()).toLocalDate()
            val todayEpochDay = today.toEpochDay()

            dailyVibeRepository.submitCheckIn(
                dateEpochDay = todayEpochDay,
                moodEnergyIndex = state.moodEnergyIndex,
                consumedModules = state.selectedModules.map { it.name },
                sleepHours = state.sleepHours,
                note = state.note.ifBlank { null },
                completedAtEpochMillis = now.toEpochMilli(),
            )

            val streak = dailyVibeRepository.currentStreak(todayEpochDay)
            val newlyUnlocked = badgeEngine.evaluate(now.toEpochMilli()).map { it.badgeId }
            val reaction = CheckInReactionPicker.pick(
                dayOfWeek = today.dayOfWeek,
                moodEnergyIndex = state.moodEnergyIndex,
                consumedModulesCount = state.selectedModules.size,
                sleepHours = state.sleepHours,
            )

            _uiState.update {
                it.copy(
                    submitting = false,
                    result = DailyVibeResult(reaction = reaction, newlyUnlockedBadges = newlyUnlocked, streak = streak),
                )
            }
        }
    }
}
