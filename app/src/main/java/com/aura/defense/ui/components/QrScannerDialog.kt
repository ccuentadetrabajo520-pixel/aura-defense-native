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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aura.defense.tools.LinkAnalysis
import com.aura.defense.tools.LinkAnalyzer
import com.aura.defense.tools.LinkRisk
import com.aura.defense.security.SocialEngineDetector
import com.aura.defense.security.ThreatAnalysis
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraRed
import com.aura.defense.ui.AuraSurface
import com.aura.defense.ui.AuraSurfaceRaised
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

    AuraHudDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("QR Anti-Phishing", color = Color(0xFF00E5FF))
                Text("Escaneo local activo", color = AuraGreen, fontSize = 12.sp)
                Text("Aura analiza el código en este dispositivo. No se sube contenido.", color = AuraMuted, fontSize = 11.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!permissionGranted) {
                    Text("Permiso de cámara requerido")
                    Text("Aura necesita la cámara para leer códigos QR. El análisis se realiza localmente.", color = AuraMuted)
                } else if (detectedValue == null) {
                    Text("Escanear código QR", color = AuraMuted, fontSize = 12.sp)
                    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
                    ScannerFrame {
                        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxWidth().height(218.dp).clip(RoundedCornerShape(10.dp)))
                        QrCameraPreview(previewView, onDetected = { value ->
                            detectedValue = value
                            scanError = false
                        }, onFailure = { scanError = true })
                    }
                    if (scanError) Text("No se pudo leer el código QR.", color = AuraAmber)
                } else {
                    val value = detectedValue.orEmpty()
                    val url = value.takeIf(::isUrl)
                    var analysis by remember(value) { mutableStateOf<ThreatAnalysis?>(null) }
                    Text("Código detectado", color = Color(0xFF00E5FF), fontSize = 12.sp)
                    ResultCard(if (url != null) "Enlace detectado" else "Texto detectado", Color(0xFF00E5FF)) {
                        Text(value, maxLines = 3, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                    if (url != null) {
                        TextButton(onClick = {
                            analysis = SocialEngineDetector.analyze(value)
                            onAnalysis(LinkAnalyzer(context).analyze(url))
                        }) { Text("Analizar enlace") }
                    } else {
                        TextButton(onClick = { analysis = SocialEngineDetector.analyze(value) }) { Text("Analizar contenido") }
                    }
                        analysis?.let { result ->
                            ResultCard("Resultado: ${result.level} (${result.score}/100)", socialLevelColor(result.level)) {
                                Text(result.reason, color = AuraMuted, fontSize = 12.sp)
                                Text("No abrir automáticamente", color = AuraMuted, fontSize = 11.sp)
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
private fun ScannerFrame(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "qr-line")
    val linePosition by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Reverse),
        label = "qr-line-position"
    )
    Box(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.55f), RoundedCornerShape(12.dp)).padding(5.dp)
    ) {
        content()
        Canvas(Modifier.matchParentSize()) {
            val length = 22.dp.toPx()
            val stroke = 2.dp.toPx()
            val corner = Color(0xFF00E5FF)
            drawLine(corner, Offset(10.dp.toPx(), length), Offset(10.dp.toPx(), 10.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(corner, Offset(10.dp.toPx(), 10.dp.toPx()), Offset(length, 10.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(corner, Offset(size.width - length, 10.dp.toPx()), Offset(size.width - 10.dp.toPx(), 10.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(corner, Offset(size.width - 10.dp.toPx(), 10.dp.toPx()), Offset(size.width - 10.dp.toPx(), length), stroke, StrokeCap.Round)
            drawLine(corner, Offset(10.dp.toPx(), size.height - length), Offset(10.dp.toPx(), size.height - 10.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(corner, Offset(10.dp.toPx(), size.height - 10.dp.toPx()), Offset(length, size.height - 10.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(corner, Offset(size.width - length, size.height - 10.dp.toPx()), Offset(size.width - 10.dp.toPx(), size.height - 10.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(corner, Offset(size.width - 10.dp.toPx(), size.height - length), Offset(size.width - 10.dp.toPx(), size.height - 10.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(AuraGreen.copy(alpha = 0.9f), Offset(18.dp.toPx(), size.height * linePosition), Offset(size.width - 18.dp.toPx(), size.height * linePosition), 1.5.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun ResultCard(title: String, accent: Color, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = accent, fontSize = 15.sp)
            content()
        }
    }
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

private fun socialLevelColor(level: String) = when (level) {
    "Critical", "High" -> AuraRed
    "Medium" -> AuraAmber
    else -> AuraGreen
}