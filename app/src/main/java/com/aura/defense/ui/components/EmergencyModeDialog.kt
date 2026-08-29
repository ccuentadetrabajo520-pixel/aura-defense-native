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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.apps.AppScanResult
import com.aura.defense.apps.InstalledAppInfo
import com.aura.defense.guardian.AuraGuardianAssessment
import com.aura.defense.guardian.toSpanish
import com.aura.defense.security.PostureResult
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraMuted

data class EmergencyModeResult(
    val posture: PostureResult,
    val appScan: AppScanResult,
    val guardian: AuraGuardianAssessment,
    val recentLinkCount: Int,
    val vpnStatus: String,
    val reportText: String,
    val reportJson: String,
    val reportSaved: Boolean
)

@Composable
fun EmergencyModeDialog(
    steps: List<String>,
    currentStep: Int,
    running: Boolean,
    result: EmergencyModeResult?,
    onAppDetails: (InstalledAppInfo) -> Unit,
    onAppPermissions: (InstalledAppInfo) -> Unit,
    onUninstall: (InstalledAppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var showingReport by remember { mutableStateOf(false) }
    AuraHudDialog(
        onDismissRequest = { if (!running) onDismiss() },
        title = { Text("Modo emergencia") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                steps.forEachIndexed { index, step ->
                    val state = when {
                        result != null -> "Completado"
                        index < currentStep -> "Completado"
                        index == currentStep && running -> "En curso"
                        else -> "Pendiente"
                    }
                    Text("$step: $state", color = if (state == "En curso") AuraCyan else AuraMuted, fontSize = 13.sp)
                }
                result?.let { emergency ->
                    Text("Resumen real", color = AuraCyan, fontSize = 16.sp)
                    Text("Postura: ${emergency.posture.status} (${emergency.posture.score.takeIf { it >= 0 } ?: "No disponible"})")
                    Text("Apps visibles: ${emergency.appScan.apps.size}")
                    Text("Apps con señales de riesgo: ${emergency.appScan.riskyApps.size}")
                    Text("Enlaces recientes revisados: ${emergency.recentLinkCount}")
                    Text("VPN: ${emergency.vpnStatus}")
                    Text("Guardián Aura: ${emergency.guardian.level.toSpanish()}", color = MaterialTheme.colorScheme.onSurface)
                    Text(emergency.guardian.summary, color = AuraMuted)

                    Text("Apps prioritarias", color = AuraCyan, fontSize = 16.sp)
                    val topApps = emergency.appScan.riskyApps
                        .sortedByDescending { app -> app.findings.maxOfOrNull { finding -> finding.severity } }
                        .take(3)
                    if (topApps.isEmpty()) {
                        Text("No se encontraron apps con señales de riesgo.", color = AuraMuted)
                    } else {
                        topApps.forEach { app ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(app.appName, color = MaterialTheme.colorScheme.onSurface)
                                Text(app.findings.joinToString(" · ") { it.reason }, color = AuraMuted, fontSize = 12.sp)
                                TextButton(onClick = { onAppDetails(app) }) { Text("Abrir detalles") }
                                TextButton(onClick = { onAppPermissions(app) }) { Text("Abrir permisos") }
                                TextButton(onClick = { onUninstall(app) }) { Text("Solicitar desinstalación") }
                            }
                        }
                    }
                    Text("Informe local", color = AuraCyan, fontSize = 16.sp)
                    Text(if (emergency.reportSaved) "Informe TXT y JSON guardados en el almacenamiento privado de Aura." else "No se pudo guardar el informe local.", color = if (emergency.reportSaved) AuraMuted else MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    TextButton(onClick = { showingReport = !showingReport }) {
                        Text(if (showingReport) "Ocultar informe generado" else "Ver informe generado")
                    }
                    if (showingReport) Text(emergency.reportText, fontSize = 12.sp, color = AuraMuted)
                }
            }
        },
        confirmButton = {
            if (!running) TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

