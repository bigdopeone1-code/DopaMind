package com.dopamind.app.core.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * The app's single ViewModel-acquisition helper: every screen's ViewModel is
 * constructed from [AppContainer] via this, so there's exactly one DI
 * pattern in the codebase instead of a mix of ad-hoc factories.
 */
@Composable
inline fun <reified T : ViewModel> dopaMindViewModel(
    crossinline create: (AppContainer) -> T,
): T {
    val container = LocalAppContainer.current
    val factory = viewModelFactory {
        initializer { create(container) }
    }
    return viewModel(factory = factory)
}
