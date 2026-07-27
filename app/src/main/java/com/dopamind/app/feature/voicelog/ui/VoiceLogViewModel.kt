package com.dopamind.app.feature.voicelog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopamind.app.core.ai.voice.ParsedCategory
import com.dopamind.app.core.ai.voice.VoiceCaptureManager
import com.dopamind.app.core.ai.voice.VoiceCaptureState
import com.dopamind.app.core.ai.voice.VoiceLogParser
import com.dopamind.app.core.ai.voice.VoiceParseResult
import com.dopamind.app.feature.alcohol.data.AlcoholRepository
import com.dopamind.app.feature.alcohol.data.DrinkType
import com.dopamind.app.feature.cannabis.data.CannabisRepository
import com.dopamind.app.feature.cannabis.data.ConsumptionMethod
import com.dopamind.app.feature.tobacco.data.TobaccoProductType
import com.dopamind.app.feature.tobacco.data.TobaccoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface VoiceLogUiState {
    data object Idle : VoiceLogUiState
    data object Listening : VoiceLogUiState
    data class Parsed(val result: VoiceParseResult) : VoiceLogUiState
    data object Saved : VoiceLogUiState
    data class Error(val message: String) : VoiceLogUiState
}

class VoiceLogViewModel(
    private val voiceCaptureManager: VoiceCaptureManager,
    private val cannabisRepository: CannabisRepository,
    private val tobaccoRepository: TobaccoRepository,
    private val alcoholRepository: AlcoholRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<VoiceLogUiState>(VoiceLogUiState.Idle)
    val state: StateFlow<VoiceLogUiState> = _state.asStateFlow()

    fun startListening() {
        _state.value = VoiceLogUiState.Listening
        viewModelScope.launch {
            voiceCaptureManager.listen().collect { captureState ->
                when (captureState) {
                    is VoiceCaptureState.FinalResult -> {
                        val parsed = VoiceLogParser.parse(captureState.transcript)
                        _state.value = VoiceLogUiState.Parsed(parsed)
                    }
                    is VoiceCaptureState.Error -> _state.value = VoiceLogUiState.Error(captureState.message)
                    else -> Unit
                }
            }
        }
    }

    fun confirmAndSave(result: VoiceParseResult) {
        viewModelScope.launch {
            result.items.forEach { item ->
                repeat(item.quantity) {
                    when (item.category) {
                        ParsedCategory.CANNABIS -> cannabisRepository.logConsumption(
                            strainName = "—",
                            method = ConsumptionMethod.JOINT,
                            thcPercent = null,
                            cbdPercent = null,
                            quantityGrams = null,
                            moodBefore = 3,
                            moodAfter = null,
                            note = "Voice log",
                        )
                        ParsedCategory.TOBACCO -> tobaccoRepository.logPuff(TobaccoProductType.IQOS_STICK, trigger = null, note = "Voice log")
                        ParsedCategory.ALCOHOL -> alcoholRepository.logDrink(
                            drinkType = DrinkType.BEER,
                            volumeMl = DrinkType.BEER.defaultVolumeMl,
                            abvPercent = DrinkType.BEER.defaultAbvPercent,
                            priceEuros = null,
                            venue = null,
                        )
                    }
                }
            }
            _state.value = VoiceLogUiState.Saved
        }
    }

    fun reset() {
        _state.value = VoiceLogUiState.Idle
    }
}
