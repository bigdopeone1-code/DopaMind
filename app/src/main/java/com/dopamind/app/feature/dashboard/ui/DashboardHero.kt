package com.dopamind.app.feature.dashboard.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dopamind.app.core.navigation.Destination
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.HeroCyan
import com.dopamind.app.core.theme.HeroPink
import com.dopamind.app.core.theme.HeroPurple
import com.dopamind.app.core.theme.Warning
import kotlin.math.hypot
import kotlin.math.min

/** Just the body visual (the reference brief's "center zone") — the greeting/
 * score/notification row lives in DashboardScreen's separate top zone.
 * Renders the real SceneView/Filament 3D model (Phase C/D) — see
 * Model3DHero.kt for the tap-to-navigate region mapping. The 2D
 * HolographicBody below this function is kept as a documented fallback per
 * the plan, not called today. */
@Composable
fun DashboardHero(onNavigate: (Destination) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Model3DHero(onNavigate = onNavigate, modifier = Modifier.fillMaxSize())
    }
}

// Body-region hotspots: each maps a fractional position on the silhouette to
// one of DopaMind's own modules (never real organs — this app is explicitly
// not a medical app), reusing the app's existing accent palette.
private data class BodyHotspot(
    val centers: List<Offset>, // fractions of (width, height)
    val color: Color,
    val destination: Destination,
    val radiusFraction: Float,
)

private val Hotspots = listOf(
    BodyHotspot(listOf(Offset(0.5f, 0.24f)), HeroPurple, Destination.DopamineFocusHome, 0.085f),
    BodyHotspot(listOf(Offset(0.40f, 0.40f), Offset(0.60f, 0.40f)), HeroCyan, Destination.TobaccoHome, 0.045f),
    BodyHotspot(listOf(Offset(0.5f, 0.48f)), HeroPink, Destination.LibidoHome, 0.05f),
    BodyHotspot(listOf(Offset(0.63f, 0.55f)), Warning, Destination.AlcoholHome, 0.045f),
    BodyHotspot(listOf(Offset(0.40f, 0.60f)), Accent, Destination.RecoveryHome, 0.045f),
)

@Composable
private fun HolographicBody(onNavigate: (Destination) -> Unit, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "holoBody")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200), repeatMode = RepeatMode.Reverse),
        label = "pulse",
    )

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { tapOffset ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                val hitRadiusPx = min(w, h) * 0.14f
                Hotspots.forEach { hotspot ->
                    hotspot.centers.forEach { fraction ->
                        val center = Offset(fraction.x * w, fraction.y * h)
                        if (hypot((tapOffset.x - center.x).toDouble(), (tapOffset.y - center.y).toDouble()) <= hitRadiusPx) {
                            onNavigate(hotspot.destination)
                        }
                    }
                }
            }
        },
    ) {
        val w = size.width
        val h = size.height
        val bodyPath = buildBodySilhouette(w, h)

        // Calm, mostly-transparent silhouette — not the focus, just a subtle
        // translucent outline the module glows sit on top of.
        drawPath(path = bodyPath, color = Color.White.copy(alpha = 0.12f), style = Stroke(width = 1.dp.toPx()))

        // Module hotspots — soft glows, mapped to DopaMind modules (never
        // real organs — this app is explicitly not a medical app).
        Hotspots.forEach { hotspot ->
            val radius = min(w, h) * hotspot.radiusFraction * pulse
            hotspot.centers.forEach { fraction ->
                val center = Offset(fraction.x * w, fraction.y * h)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(hotspot.color.copy(alpha = 0.55f), hotspot.color.copy(alpha = 0f)),
                        center = center,
                        radius = radius * 1.2f,
                    ),
                    radius = radius * 1.2f,
                    center = center,
                )
                drawCircle(color = hotspot.color.copy(alpha = 0.8f), radius = radius * 0.28f, center = center)
            }
        }
    }
}

/**
 * A simplified, stylized standing humanoid outline (front-facing, arms at the
 * sides, legs together) built from a handful of symmetric primitives — not
 * anatomically precise, just enough to read as a body for the hologram effect.
 */
private fun buildBodySilhouette(w: Float, h: Float): Path = Path().apply {
    // Head — kept clear of the title/subtitle row above it.
    addOval(Rect(center = Offset(w * 0.5f, h * 0.24f), radius = w * 0.085f))
    // Neck.
    addRect(Rect(w * 0.46f, h * 0.30f, w * 0.54f, h * 0.335f))
    // Torso (shoulders to waist) — shoulder edges overlap the arms below so
    // the limbs read as attached rather than floating.
    moveTo(w * 0.28f, h * 0.335f)
    lineTo(w * 0.72f, h * 0.335f)
    lineTo(w * 0.62f, h * 0.60f)
    lineTo(w * 0.38f, h * 0.60f)
    close()
    // Shoulder joints — small circles smoothing the torso-to-arm connection.
    addOval(Rect(center = Offset(w * 0.28f, h * 0.345f), radius = w * 0.025f))
    addOval(Rect(center = Offset(w * 0.72f, h * 0.345f), radius = w * 0.025f))
    // Hips.
    moveTo(w * 0.38f, h * 0.60f)
    lineTo(w * 0.62f, h * 0.60f)
    lineTo(w * 0.64f, h * 0.65f)
    lineTo(w * 0.36f, h * 0.65f)
    close()
    // Arms — inner edge overlaps the torso's shoulder edge by design.
    addRoundRect(RoundRectShape(w * 0.16f, h * 0.335f, w * 0.29f, h * 0.63f, w * 0.03f))
    addRoundRect(RoundRectShape(w * 0.71f, h * 0.335f, w * 0.84f, h * 0.63f, w * 0.03f))
    // Hands.
    addOval(Rect(center = Offset(w * 0.225f, h * 0.645f), radius = w * 0.045f))
    addOval(Rect(center = Offset(w * 0.775f, h * 0.645f), radius = w * 0.045f))
    // Legs.
    addRoundRect(RoundRectShape(w * 0.38f, h * 0.65f, w * 0.485f, h * 0.93f, w * 0.025f))
    addRoundRect(RoundRectShape(w * 0.515f, h * 0.65f, w * 0.62f, h * 0.93f, w * 0.025f))
    // Feet.
    addOval(Rect(w * 0.395f, h * 0.935f, w * 0.49f, h * 0.965f))
    addOval(Rect(w * 0.51f, h * 0.935f, w * 0.605f, h * 0.965f))
}

private fun RoundRectShape(left: Float, top: Float, right: Float, bottom: Float, corner: Float) =
    androidx.compose.ui.geometry.RoundRect(Rect(left, top, right, bottom), CornerRadius(corner, corner))
