package dev.krydo.mobile.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "KrydoCamera"

@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var deniedPermanently by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { ok ->
        granted = ok
        if (!ok) deniedPermanently = true
    }

    return CameraPermissionState(
        granted = granted,
        deniedPermanently = deniedPermanently && !granted,
        request = { launcher.launch(Manifest.permission.CAMERA) },
    )
}

data class CameraPermissionState(
    val granted: Boolean,
    val deniedPermanently: Boolean,
    val request: () -> Unit,
)

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onQrDetected: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var bindError by remember { mutableStateOf<String?>(null) }
    val handled = remember { AtomicBoolean(false) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(dev.krydo.mobile.ui.theme.KrydoColors.BackgroundPrimary),
        contentAlignment = Alignment.Center,
    ) {
        if (!enabled) {
            Text(
                "Camera off",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
            return
        }

        val previewView = remember {
            PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }

        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        LaunchedEffect(enabled, onQrDetected) {
            if (!enabled) return@LaunchedEffect
            handled.set(false)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                {
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val useCases = mutableListOf<androidx.camera.core.UseCase>(preview)
                        if (onQrDetected != null) {
                            val options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                            val scanner = BarcodeScanning.getClient(options)
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            analysis.setAnalyzer(
                                analysisExecutor,
                                MlKitAnalyzer(
                                    listOf(scanner),
                                    ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                                    ContextCompat.getMainExecutor(context),
                                ) { result ->
                                    val barcodes = result?.getValue(scanner).orEmpty()
                                    val raw = barcodes.firstOrNull()?.rawValue ?: return@MlKitAnalyzer
                                    if (handled.compareAndSet(false, true)) {
                                        onQrDetected(raw)
                                    }
                                },
                            )
                            useCases += analysis
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            *useCases.toTypedArray(),
                        )
                        bindError = null
                    } catch (err: Exception) {
                        Log.e(TAG, "Camera bind failed", err)
                        bindError = err.message ?: "Camera failed"
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                runCatching {
                    ProcessCameraProvider.getInstance(context).get().unbindAll()
                }
                analysisExecutor.shutdown()
            }
        }

        bindError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
