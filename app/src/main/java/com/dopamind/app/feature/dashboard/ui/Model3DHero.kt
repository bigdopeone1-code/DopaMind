package com.dopamind.app.feature.dashboard.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dopamind.app.core.navigation.Destination
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView

/**
 * Real 3D rendering (SceneView/Filament): a placeholder animated humanoid
 * model (CesiumMan, CC BY 4.0 — see THIRD_PARTY_LICENSES_CesiumMan_CC-BY-4.0.txt),
 * pinch-zoom/pan/rotate via the default camera manipulator.
 *
 * Tap-to-navigate: no free CC0/CC-BY asset with separate named meshes per
 * body region turned up (Sketchfab/Meshy require gated downloads), so taps
 * are mapped to a module by the 3D world-space height of the hit point
 * instead of by mesh identity — still a real tap on the real model, just a
 * coarser region test. Swapping in a properly-segmented model later (Phase D)
 * only requires replacing [regionForWorldY]'s thresholds with real mesh names.
 */
@Composable
fun Model3DHero(onNavigate: (Destination) -> Unit, modifier: Modifier = Modifier) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraManipulator = rememberCameraManipulator()

    Scene(
        modifier = modifier.fillMaxSize(),
        engine = engine,
        view = rememberView(engine),
        renderer = rememberRenderer(engine),
        scene = rememberScene(engine),
        cameraManipulator = cameraManipulator,
        childNodes = rememberNodes {
            add(
                ModelNode(
                    modelInstance = modelLoader.createModelInstance(assetFileLocation = "models/cesium_man.glb"),
                    scaleToUnits = 0.5f,
                    centerOrigin = Position(y = 0f),
                )
            )
        },
        onTouchEvent = { _, hitResult ->
            hitResult?.let { onNavigate(regionForWorldY(it.worldPosition.y)) }
            false
        },
    )
}

/** Coarse head-to-toe region mapping over the model's centered ~0.5-unit-tall bounding box. */
private fun regionForWorldY(y: Float): Destination = when {
    y > 0.15f -> Destination.DopamineFocusHome
    y > 0.05f -> Destination.TobaccoHome
    y > -0.05f -> Destination.LibidoHome
    y > -0.15f -> Destination.AlcoholHome
    else -> Destination.RecoveryHome
}
