package com.dopamind.app.core.theme

import androidx.compose.ui.graphics.Color

// Background — calm/premium palette (Samsung Health / Oura / WHOOP / Flo
// inspired), not the earlier neon-cyberpunk one.
val BackgroundPrimary = Color(0xFF1F2023)
val BackgroundElevated = Color(0xFF2A2B30)

// Border
val BorderSubtle = Color(0xFF35363B)
val BorderHover = Color(0xFF44454B)

// Text
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF9FA3AE)
val TextDisabled = Color(0xFF5C5F66)

// Accent — the single accent color for CTAs, progress rings, and highlights
val Accent = Color(0xFF4ADE80)

// Semantic — reserved for real states only, never decorative
val Warning = Color(0xFFF5B55A)
val Danger = Color(0xFFFF6B6B)

// Hero / module-hotspot accents — used only by the Dashboard body hotspots
// and chart palettes, never by buttons/CTAs (Accent remains the one CTA
// color). Named "Hero" for historical reasons; values match the calm
// palette, not neon.
val HeroBlue = Color(0xFF6C8DFF)
val HeroCyan = Color(0xFF55D8C1)
val HeroPink = Color(0xFFFF6B6B)
val HeroPurple = Color(0xFFA77CFF)
