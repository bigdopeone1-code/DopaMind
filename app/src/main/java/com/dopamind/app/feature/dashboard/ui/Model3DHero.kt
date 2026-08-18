package com.dopamind.app.feature.dashboard.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dopamind.app.core.navigation.Destination
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView

/**
 * Real 3D rendering (SceneView/Filament): DopaMind's own bespoke anatomical
 * body model (models/dopamind_body.glb — custom asset, not third-party;
 * its own glTF extras mark it "Stylized prototype; not medically accurate
 * and not for clinical use", consistent with this app's non-medical stance),
 * pinch-zoom/pan/rotate via the default camera manipulator.
 *
 * The source mesh was authored Z-up (height along Z, exported by trimesh,
 * which doesn't re-orient to glTF's Y-up convention) — `rotation = Rotation(x = -90f)`
 * below corrects that so the body stands upright in SceneView's Y-up world
 * instead of lying on its back.
 *
 * Tap-to-navigate: the model carries real named per-organ meshes (Brain_*,
 * Lung_Left/Right, Heart, Liver, Kidney_Left/Right, Stomach, Digestive_*)
 * under a translucent body shell, but SceneView's touch callback here only
 * hands back a world-space hit position, not the mesh/node that was hit — so
 * taps are resolved by the hit point's position instead of by mesh identity.
 * [regionForBodyHit]'s thresholds are derived from this model's actual
 * bounding boxes (see the comment above it), not guessed, so they land on
 * the right organ cluster for anything except the chest, where the heart
 * and lungs occupy nearly the same height and can only be told apart by how
 * close to the centerline the tap landed. A proper per-mesh hit test (once
 * we confirm what SceneView's hit-result type actually exposes) would fix
 * that ambiguity — left for the next interaction pass.
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
                    modelInstance = modelLoader.createModelInstance(assetFileLocation = "models/dopamind_body.glb"),
                    scaleToUnits = 0.5f,
                    centerOrigin = Position(y = 0f),
                ).apply {
                    // Corrects the source mesh's Z-up authoring (see kdoc above) —
                    // set post-construction since ModelNode's constructor parameter
                    // list isn't pinned down here; `rotation` is a standard mutable
                    // Node property so this is the safer of the two call shapes.
                    rotation = Rotation(x = -90f)
                }
            )
        },
        onTouchEvent = { _, hitResult ->
            hitResult?.let { onNavigate(regionForBodyHit(it.worldPosition.x, it.worldPosition.y)) }
            false
        },
    )
}

/**
 * Body-region -> module mapping, calibrated against models/dopamind_body.glb's
 * real mesh bounding boxes (not eyeballed): after `scaleToUnits = 0.5f` +
 * `centerOrigin` + the -90° X rotation above, each organ cluster's original
 * bounding box maps to a world-Y band as follows (head at the top, legs at
 * the bottom, matching the model's own "interactive_organs" list):
 *
 *   Head/Brain                 y in (0.155, 0.25]   -> Dopamine & Focus
 *   Chest (Lungs + Heart)      y in (0.067, 0.155]  -> Tobacco/IQOS or Libido, split by |x|
 *   Abdomen (Liver/Kidney/Stomach) y in (0.015, 0.067] -> Alcohol & Party
 *   Lower torso (Digestive/Pelvis) y in (-0.03, 0.015] -> Recovery & Food
 *   Legs                       y <= -0.03            -> Recovery & Food
 *
 * Inside the chest band, the heart sits centered (|x| roughly within 0.02 of
 * the midline) while the lungs flank it on both sides — so a narrow-center
 * tap resolves to Libido (heart), a lateral one to Tobacco (lungs).
 */
private const val HEAD_Y_THRESHOLD = 0.155f
private const val CHEST_Y_THRESHOLD = 0.067f
private const val ABDOMEN_Y_THRESHOLD = 0.015f
private const val HEART_CENTERLINE_HALF_WIDTH = 0.02f

private fun regionForBodyHit(x: Float, y: Float): Destination = when {
    y > HEAD_Y_THRESHOLD -> Destination.DopamineFocusHome
    y > CHEST_Y_THRESHOLD -> if (kotlin.math.abs(x) <= HEART_CENTERLINE_HALF_WIDTH) {
        Destination.LibidoHome
    } else {
        Destination.TobaccoHome
    }
    y > ABDOMEN_Y_THRESHOLD -> Destination.AlcoholHome
    // Lower torso (digestive/pelvis) and legs both fall through to Recovery & Food —
    // the module's own scope already covers food/digestion-adjacent tracking.
    else -> Destination.RecoveryHome
}
