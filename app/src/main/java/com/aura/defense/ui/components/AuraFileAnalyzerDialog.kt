package com.aura.defense.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.files.AuraFileAnalysis
import com.aura.defense.files.AuraFileAnalyzer
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraRed

@Composable
fun AuraFileAnalyzerDialog(initialUri: Uri? = null, onAnalysis: (AuraFileAnalysis) -> Unit = {}, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var analysis by remember(initialUri) { mutableStateOf(initialUri?.let { AuraFileAnalyzer(context).analyze(it) }) }
    analysis?.let(onAnalysis)
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        analysis = uri?.let { AuraFileAnalyzer(context).analyze(it) }
    }
    AuraHudDialog(
        onDismissRequest = onDismiss,
        title = { Text("Analizador de archivos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Selecciona un archivo o comparte un APK con AURA DEFENS. No se ejecutó el archivo.", color = AuraMuted, fontSize = 12.sp)
                analysis?.let { AuraFileResult(it) } ?: Text("No hay archivo seleccionado", color = AuraMuted)
            }
        },
        confirmButton = { Button(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Seleccionar archivo") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun AuraFileResult(result: AuraFileAnalysis) {
    val color = when (result.risk) { "Riesgo potencial" -> AuraAmber; "No disponible" -> AuraRed; else -> AuraCyan }
    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(if (result.isApk) "APK analizada" else "Archivo analizado", color = AuraCyan, fontSize = 15.sp)
        Text(result.name, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
        Text("Tamaño: ${result.size} bytes · SHA-256: ${result.sha256}", color = AuraMuted, fontSize = 11.sp)
        result.packageName?.let { Text("Paquete: $it · Versión: ${result.version ?: "No disponible"}", color = AuraMuted, fontSize = 12.sp) }
        Text(result.risk, color = color, fontSize = 14.sp)
        result.reasons.forEach { Text("• $it", color = AuraMuted, fontSize = 12.sp) }
        if (result.sensitivePermissions.isNotEmpty()) Text("Permisos sensibles: ${result.sensitivePermissions.size}", color = AuraRed, fontSize = 12.sp)
        Text("Revisión recomendada", color = AuraMuted, fontSize = 11.sp)
    }
}
