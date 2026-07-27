package com.dopamind.app.feature.weeklyrecap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopamind.app.feature.weeklyrecap.domain.RecapCard
import com.dopamind.app.feature.weeklyrecap.domain.WeeklyRecapGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeeklyRecapViewModel(private val generator: WeeklyRecapGenerator) : ViewModel() {
    private val _cards = MutableStateFlow<List<RecapCard>?>(null)
    val cards: StateFlow<List<RecapCard>?> = _cards.asStateFlow()

    init {
        viewModelScope.launch { _cards.value = generator.generate() }
    }
}
