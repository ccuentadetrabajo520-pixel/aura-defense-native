package com.aura.defense.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.apps.AppRiskSeverity
import com.aura.defense.apps.InstalledAppInfo
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraMuted

@Composable
fun AppRisksDialog(
    apps: List<InstalledAppInfo>,
    onDetails: (InstalledAppInfo) -> Unit,
    onPermissions: (InstalledAppInfo) -> Unit,
    onUninstall: (InstalledAppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    AuraHudDialog(
        onDismissRequest = onDismiss,
        title = { Text("Riesgos de apps") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (apps.isEmpty()) {
                    Text("No hay apps con señales de riesgo en el último escaneo.", color = AuraMuted)
                } else {
                    apps.forEach { app ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(app.appName, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                            Text(app.packageName, color = AuraMuted, fontSize = 11.sp)
                            Text("Severidad: ${app.findings.maxOf { it.severity }.toSpanish()}", color = AuraCyan, fontSize = 12.sp)
                            Text(app.findings.joinToString(" · ") { it.reason }, color = AuraMuted, fontSize = 12.sp)
                            TextButton(onClick = { onDetails(app) }) { Text("Abrir detalles") }
                            TextButton(onClick = { onPermissions(app) }) { Text("Abrir permisos") }
                            TextButton(onClick = { onUninstall(app) }) { Text("Solicitar desinstalación") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

private fun AppRiskSeverity.toSpanish(): String = when (this) {
    AppRiskSeverity.LOW -> "Baja"
    AppRiskSeverity.MEDIUM -> "Media"
    AppRiskSeverity.HIGH -> "Alta"
    AppRiskSeverity.CRITICAL -> "Crítica"
}
