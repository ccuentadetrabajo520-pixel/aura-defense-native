package com.aura.defense.ai.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aura.defense.ui.components.AuraButton

@Composable
fun VoiceCommandScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val engine = remember { VoiceCommandEngine(context) }
    val commandResult by engine.commandResult.collectAsState()
    val isProcessing by engine.isProcessing.collectAsState()
    val error by engine.error.collectAsState()
    val hasMicrophonePermission = remember {
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val permissionState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(hasMicrophonePermission) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionState.value = granted
        if (granted) engine.startListening()
    }

    DisposableEffect(engine) {
        onDispose { engine.destroy() }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Comandos de voz", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.size(190.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isProcessing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isProcessing) "Escuchando" else "Listo", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(if (isProcessing) "Habla ahora" else "Toca activar")
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AuraButton(
                text = if (isProcessing) "Detener" else "Activar",
                onClick = {
                    if (isProcessing) engine.stopListening()
                    else if (permissionState.value) engine.startListening()
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier.weight(1f)
            )
            AuraButton(
                text = "Limpiar",
                onClick = engine::clearResult,
                modifier = Modifier.weight(1f)
            )
        }
        commandResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Comando detectado", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(result, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text(
            "Prueba: “Escanea mi teléfono”, “Activa la VPN”, “Qué amenazas hay” o “Muéstrame los reportes”.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun VoiceCommandDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            VoiceCommandScreen(modifier = Modifier.height(620.dp))
        }
    }
}