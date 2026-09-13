package com.aura.defense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aura.defense.data.repository.SecurityRepository
import com.aura.defense.engine.MalwareDetectionEngine
import com.aura.defense.engine.NetworkAnalyzer
import kotlinx.coroutines.launch

@Composable
fun ScanScreen() {
    val context = LocalContext.current
    val repository = remember { SecurityRepository(context) }
    val engine = remember { MalwareDetectionEngine(context) }
    val networkAnalyzer = remember { NetworkAnalyzer(context) }
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Escaneo de malware", style = MaterialTheme.typography.headlineMedium)
        Text(scanResult ?: "Listo para analizar las aplicaciones instaladas.")
        Button(
            onClick = {
                isScanning = true
                scanResult = null
                scope.launch {
                    val appThreats = engine.scanInstalledApps()
                    val networkThreats = networkAnalyzer.analyzeNetworkSecurity()
                    val threats = appThreats + networkThreats
                    threats.forEach(repository::addThreat)
                    scanResult = if (threats.isEmpty()) {
                        "No se detectaron amenazas."
                    } else {
                        "Amenazas detectadas: ${threats.size}"
                    }
                    isScanning = false
                }
            },
            enabled = !isScanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isScanning) "Escaneando..." else "Iniciar escaneo")
        }
    }
}
