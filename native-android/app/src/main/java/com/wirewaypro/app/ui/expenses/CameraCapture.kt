package com.wirewaypro.app.ui.expenses

import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

/**
 * In-app CameraX preview + capture. Writes each photo to cache/receipts and hands
 * back its file Uri. Includes a document framing guide (fit the receipt inside the
 * frame) and a torch toggle for low light.
 *
 * Two modes:
 * - single (default): one shutter press returns one Uri via [onCaptured] and the
 *   caller is expected to dismiss the camera.
 * - batch ([batchMode] = true): each shutter press accumulates a Uri while the
 *   preview stays open; tapping Done returns the whole list via [onBatchDone].
 */
@Composable
fun CameraCapture(
    onClose: () -> Unit,
    onCaptured: (Uri) -> Unit = {},
    batchMode: Boolean = false,
    onBatchDone: (List<Uri>) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    val batch = remember { mutableStateListOf<Uri>() }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    runCatching {
                        provider.unbindAll()
                        camera = provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                        )
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )

        // Document framing guide — a rounded rectangle the user aligns the receipt to.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.82f)
                .fillMaxHeight(0.62f)
                .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)), RoundedCornerShape(14.dp)),
        )

        Text(
            text = if (batchMode) "Snap each receipt — tap Done when finished" else "Fit the whole receipt inside the frame",
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp, start = 24.dp, end = 24.dp),
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
        }

        IconButton(
            onClick = {
                torchOn = !torchOn
                camera?.cameraControl?.enableTorch(torchOn)
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        ) {
            Icon(
                if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                contentDescription = "Toggle flash",
                tint = Color.White,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (batchMode && batch.isNotEmpty()) {
                Text(
                    text = "${batch.size} captured",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FloatingActionButton(
                    onClick = {
                        if (capturing) return@FloatingActionButton
                        capturing = true
                        capture(
                            context = context,
                            imageCapture = imageCapture,
                            onSaved = { uri -> if (batchMode) batch.add(uri) else onCaptured(uri) },
                            onSettled = { capturing = false },
                        )
                    },
                    modifier = Modifier.size(72.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Capture")
                }
                if (batchMode && batch.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { onBatchDone(batch.toList()) },
                        icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                        text = { Text("Done (${batch.size})") },
                    )
                }
            }
        }
    }
}

private fun capture(
    context: android.content.Context,
    imageCapture: ImageCapture,
    onSaved: (Uri) -> Unit,
    onSettled: () -> Unit,
) {
    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val file = File(dir, "cam_${System.currentTimeMillis()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onSettled()
                onSaved(Uri.fromFile(file))
            }

            override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                // Swallowed — the user can retry; the form still works without a photo.
                onSettled()
            }
        },
    )
}
