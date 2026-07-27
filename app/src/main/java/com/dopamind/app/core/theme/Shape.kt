package com.dopamind.app.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Radius scale: 16dp cards, 12dp buttons, 8dp chips
val CardShape = RoundedCornerShape(16.dp)
val ButtonShape = RoundedCornerShape(12.dp)
val ChipShape = RoundedCornerShape(8.dp)

val DopaMindShapes = Shapes(
    extraSmall = ChipShape,
    small = ChipShape,
    medium = ButtonShape,
    large = CardShape,
    extraLarge = CardShape,
)
