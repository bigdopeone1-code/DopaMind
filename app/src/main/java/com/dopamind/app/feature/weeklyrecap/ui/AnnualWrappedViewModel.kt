package com.dopamind.app.feature.weeklyrecap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopamind.app.feature.weeklyrecap.domain.AnnualWrapped
import com.dopamind.app.feature.weeklyrecap.domain.AnnualWrappedGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnnualWrappedViewModel(private val generator: AnnualWrappedGenerator) : ViewModel() {
    private val _wrapped = MutableStateFlow<AnnualWrapped?>(null)
    val wrapped: StateFlow<AnnualWrapped?> = _wrapped.asStateFlow()

    init {
        viewModelScope.launch { _wrapped.value = generator.generate() }
    }
}
