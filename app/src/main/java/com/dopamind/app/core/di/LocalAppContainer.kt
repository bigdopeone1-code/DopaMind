package com.dopamind.app.core.di

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("No AppContainer provided — wrap the app content in CompositionLocalProvider(LocalAppContainer provides ...)")
}
