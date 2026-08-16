package com.dopamind.app.feature.scanner.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dopamind.app.R
import com.dopamind.app.core.designsystem.DMCard
import com.dopamind.app.core.designsystem.DMPrimaryButton
import com.dopamind.app.core.di.dopaMindViewModel
import com.dopamind.app.core.navigation.ScanTarget
import com.dopamind.app.core.theme.Accent
import com.dopamind.app.core.theme.TextPrimary
import com.dopamind.app.core.theme.TextSecondary
import com.google.mlkit.vision.common.InputImage

@Composable
fun VisionScannerScreen(target: ScanTarget, onDone: () -> Unit) {
    val viewModel = dopaMindViewModel { container ->
        VisionScannerViewModel(target, container.alcoholRepository, container.recoveryRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    var hasCameraPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = granted
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is ScanUiState.DrinkResult -> DrinkResultContent((state as ScanUiState.DrinkResult).result, onConfirm = { viewModel.confirmDrink(it); onDone() })
            is ScanUiState.FoodResult -> FoodResultContent((state as ScanUiState.FoodResult).result, onConfirm = { viewModel.confirmFood(it); onDone() })
            is ScanUiState.Error -> ErrorContent(onDone)
            else -> {
                if (hasCameraPermission) {
                    CameraCaptureContent(scanning = state is ScanUiState.Scanning, onImageCaptured = viewModel::onImageCaptured)
                } else {
                    PermissionRationale()
                }
            }
        }

        IconButton(onClick = onDone, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close), tint = TextSecondary)
        }
    }
}

@Composable
private fun CameraCaptureContent(scanning: Boolean, onImageCaptured: (InputImage) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx -> PreviewView(ctx) },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val capture = ImageCapture.Builder().build()
                    imageCapture = capture
                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    }
                }, ContextCompat.getMainExecutor(context))
            },
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DMPrimaryButton(
                text = stringResource(if (scanning) R.string.scanner_scanning else R.string.scanner_capture),
                onClick = {
                    val capture = imageCapture
                    if (capture != null) {
                        capture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val mediaImage = image.image
                                    if (mediaImage != null) {
                                        val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                                        onImageCaptured(inputImage)
                                    }
                                    image.close()
                                }

                                override fun onError(exception: ImageCaptureException) = Unit
                            },
                        )
                    }
                },
                enabled = !scanning,
            )
        }
    }
}

@Composable
private fun PermissionRationale() {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.scanner_permission_rationale), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
    }
}

@Composable
private fun ErrorContent(onDone: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.scanner_error), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        DMPrimaryButton(text = stringResource(R.string.action_close), onClick = onDone)
    }
}

@Composable
private fun DrinkResultContent(result: com.dopamind.app.core.ai.vision.DrinkLabelScanResult, onConfirm: (com.dopamind.app.core.ai.vision.DrinkLabelScanResult) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        DMCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.scanner_drink_result_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(
                text = stringResource(
                    R.string.scanner_drink_result_body,
                    result.abvPercent ?: 0f,
                    result.volumeMl ?: 0,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = Accent,
            )
        }
        DMPrimaryButton(text = stringResource(R.string.scanner_confirm_log), onClick = { onConfirm(result) })
    }
}

@Composable
private fun FoodResultContent(result: com.dopamind.app.core.ai.vision.FoodScanResult, onConfirm: (com.dopamind.app.core.ai.vision.FoodScanResult) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        DMCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.scanner_food_result_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(text = result.topLabel ?: "—", style = MaterialTheme.typography.bodyLarge, color = Accent)
            Text(text = stringResource(R.string.recovery_munchies_score, result.junkScore), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        DMPrimaryButton(text = stringResource(R.string.scanner_confirm_log), onClick = { onConfirm(result) })
    }
}
