package com.dopamind.app.feature.scanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopamind.app.core.ai.vision.DrinkLabelScanResult
import com.dopamind.app.core.ai.vision.FoodScanResult
import com.dopamind.app.core.ai.vision.FoodScannerEngine
import com.dopamind.app.core.ai.vision.LabelScannerEngine
import com.dopamind.app.core.navigation.ScanTarget
import com.dopamind.app.feature.alcohol.data.AlcoholRepository
import com.dopamind.app.feature.alcohol.data.DrinkType
import com.dopamind.app.feature.recovery.data.MunchiesSource
import com.dopamind.app.feature.recovery.data.RecoveryRepository
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Scanning : ScanUiState
    data class DrinkResult(val result: DrinkLabelScanResult) : ScanUiState
    data class FoodResult(val result: FoodScanResult) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class VisionScannerViewModel(
    private val target: ScanTarget,
    private val alcoholRepository: AlcoholRepository,
    private val recoveryRepository: RecoveryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    fun onImageCaptured(image: InputImage) {
        if (_state.value == ScanUiState.Scanning) return
        _state.value = ScanUiState.Scanning
        viewModelScope.launch {
            try {
                when (target) {
                    ScanTarget.DRINK_LABEL -> {
                        val result = LabelScannerEngine.scan(image)
                        _state.value = ScanUiState.DrinkResult(result)
                    }
                    ScanTarget.FOOD_PLATE -> {
                        val result = FoodScannerEngine.scan(image)
                        _state.value = ScanUiState.FoodResult(result)
                    }
                }
            } catch (t: Throwable) {
                _state.value = ScanUiState.Error(t.message ?: "scan_failed")
            }
        }
    }

    fun confirmDrink(result: DrinkLabelScanResult) {
        viewModelScope.launch {
            alcoholRepository.logDrink(
                drinkType = DrinkType.OTHER,
                volumeMl = result.volumeMl ?: DrinkType.OTHER.defaultVolumeMl,
                abvPercent = result.abvPercent ?: DrinkType.OTHER.defaultAbvPercent,
                priceEuros = null,
                venue = null,
            )
        }
    }

    fun confirmFood(result: FoodScanResult) {
        viewModelScope.launch {
            recoveryRepository.logMunchies(
                foodDescription = result.topLabel ?: "—",
                junkScore = result.junkScore,
                source = MunchiesSource.VISION_SCAN,
            )
        }
    }
}
