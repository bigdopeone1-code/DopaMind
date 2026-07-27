package com.dopamind.app.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// DopaMind is dark-mode only by design (premium fitness-app aesthetic),
// regardless of the system theme setting.
private val DopaMindColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    secondary = Accent,
    onSecondary = Color.Black,
    tertiary = Warning,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = BackgroundElevated,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderHover,
    error = Danger,
    onError = Color.Black,
)

@Composable
fun DopaMindTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DopaMindColorScheme,
        typography = DopaMindTypography,
        shapes = DopaMindShapes,
        content = content,
    )
}
