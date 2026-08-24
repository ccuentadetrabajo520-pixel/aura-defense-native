package com.aura.defense.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aura.defense.tools.LinkAnalysis
import com.aura.defense.tools.LinkAnalyzer
import com.aura.defense.tools.LinkRisk
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraRed
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QrScannerDialog(onAnalysis: (LinkAnalysis) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var detectedValue by remember { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> permissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QR Anti-Phishing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!permissionGranted) {
                    Text("Permiso de cámara requerido")
                    Text("Aura necesita la cámara para leer códigos QR. El análisis se realiza localmente.", color = AuraMuted)
                } else if (detectedValue == null) {
                    Text("Escanear código QR")
                    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                    )
                    QrCameraPreview(previewView, onDetected = { value ->
                        detectedValue = value
                        scanError = false
                    }, onFailure = { scanError = true })
                    if (scanError) Text("No se pudo leer el código QR.", color = AuraAmber)
                } else {
                    val value = detectedValue.orEmpty()
                    val url = value.takeIf(::isUrl)
                    Text("Código detectado")
                    Text(if (url != null) "Enlace detectado" else "Texto detectado", color = AuraMuted)
                    Text(value)
                    if (url != null) {
                        var analysis by remember(url) { mutableStateOf<LinkAnalysis?>(null) }
                        TextButton(onClick = {
                            analysis = LinkAnalyzer().analyze(url)
                            analysis?.let(onAnalysis)
                        }) { Text("Analizar enlace") }
                        analysis?.let { result ->
                            Text("Resultado: ${result.risk.toSpanish()}", color = result.risk.color())
                            result.reasons.forEach { Text("• $it", color = AuraMuted) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                !permissionGranted -> Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Abrir cámara") }
                detectedValue == null -> TextButton(onClick = onDismiss) { Text("Cerrar") }
                else -> TextButton(onClick = { detectedValue = null; scanError = false }) { Text("Escanear código QR") }
            }
        },
        dismissButton = {
            if (!permissionGranted) {
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                }) { Text("Abrir ajustes") }
            } else if (detectedValue != null) {
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    )
}

@Composable
private fun QrCameraPreview(previewView: PreviewView, onDetected: (String) -> Unit, onFailure: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val handled = remember { AtomicBoolean(false) }
    val scanner = remember {
        com.google.mlkit.vision.barcode.BarcodeScanning.getClient(
            com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    DisposableEffect(previewView, lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            runCatching {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null || handled.get()) {
                        imageProxy.close()
                    } else {
                        val input = com.google.mlkit.vision.common.InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(input)
                            .addOnSuccessListener { codes ->
                                codes.firstNotNullOfOrNull { it.rawValue ?: it.displayValue }?.let { value ->
                                    if (handled.compareAndSet(false, true)) onDetected(value)
                                }
                            }
                            .addOnFailureListener { onFailure() }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }.onFailure { onFailure() }
        }
        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            runCatching { scanner.close() }
            executor.shutdown()
        }
    }
}

private fun isUrl(value: String): Boolean = runCatching {
    val uri = URI(value)
    (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) && !uri.host.isNullOrBlank()
}.getOrDefault(false)

private fun LinkRisk.toSpanish() = when (this) {
    LinkRisk.SEGURO -> "Seguro"
    LinkRisk.SOSPECHOSO -> "Sospechoso"
    LinkRisk.PELIGROSO -> "Peligroso"
}

private fun LinkRisk.color() = when (this) {
    LinkRisk.SEGURO -> AuraGreen
    LinkRisk.SOSPECHOSO -> AuraAmber
    LinkRisk.PELIGROSO -> AuraRed
}