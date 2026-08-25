package com.aura.defense.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraRed
import com.aura.defense.ui.AuraSurface
import com.aura.defense.ui.components.ActionButton
import com.aura.defense.ui.components.Metric
import com.aura.defense.ui.components.ModuleDialog
import com.aura.defense.ui.components.Panel
import com.aura.defense.ui.components.RadarCanvas
import com.aura.defense.ui.components.SectionTitle
import com.aura.defense.ui.components.StatusDot
import com.aura.defense.ui.components.TacticalMap
import com.aura.defense.guardian.AuraGuardianAssessment
import com.aura.defense.ui.components.AuraGuardianPanel
import com.aura.defense.apps.AppScanResult
import com.aura.defense.apps.AppRiskSeverity
import com.aura.defense.apps.InstalledAppInfo
import com.aura.defense.lan.AuraLanPeer

@Composable
fun HomeScreen(
    result: com.aura.defense.security.PostureResult,
    guardianAssessment: AuraGuardianAssessment,
    historyCount: Int,
    onGuardianAnalysis: () -> Unit,
    onStartScan: () -> Unit,
    onModuleDialog: (String, String) -> Unit,
    onEmergency: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("PANEL PRINCIPAL", "Centro de defensa Aura", "Diagnóstico local del dispositivo y sus señales disponibles.")
        AuraGuardianPanel(assessment = guardianAssessment, onViewAnalysis = onGuardianAnalysis)
        if (historyCount > 0) Text("Cambios recientes: $historyCount", color = AuraMuted, fontSize = 12.sp)
        Panel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("PUNTUACIÓN AURA", color = AuraMuted, fontSize = 11.sp)
                        Text(if (result.score >= 0) "${result.score}/100" else "No disponible", color = AuraCyan, fontSize = 24.sp)
                    }
                        StatusDot(if (result.score >= 85) AuraGreen else if (result.score >= 60) AuraAmber else AuraRed, "ESTADO", result.status)
                }
                    RadarCanvas(Modifier.fillMaxWidth().height(150.dp), result.score)
            }
        }
        Panel(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Metric("VPN", if (result.telemetry.vpnActive) "Activa" else "Inactiva", AuraAmber)
                Metric("DNS", result.telemetry.privateDnsStatus, AuraGreen)
                Metric("RIESGOS", result.findings.count { it.severity >= com.aura.defense.security.FindingSeverity.MEDIUM }.toString(), AuraRed)
            }
        }
        ActionButton("Iniciar escaneo", onClick = { onStartScan() })
        ActionButton("Activar defensa VPN", onClick = { onModuleDialog("Defensa VPN", "El servicio VPN real se implementará en la fase de cortafuegos. Aura no mostrará protección VPN activa hasta que Android confirme el servicio.") }, outlined = true)
        ActionButton("Modo emergencia", onClick = onEmergency, outlined = true)
    }
}

@Composable
fun AurasScreen(
    locationActive: Boolean,
    lanSearching: Boolean,
    lanPeers: List<AuraLanPeer>,
    lastLanScan: String?,
    visible: Boolean,
    onActivateLocation: () -> Unit,
    onVisibilityToggle: () -> Unit,
    onSearchLan: () -> Unit,
    onStopLanSearch: () -> Unit,
    onModuleDialog: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("AURAS", "Campo táctico", if (locationActive) "Ubicación activa. No se muestran coordenadas." else "Vista privada sin ubicación ni datos LAN activos.")
        TacticalMap(Modifier.fillMaxWidth().height(250.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (locationActive) "Ubicación activa" else "Ubicación no activada", color = if (locationActive) AuraGreen else AuraAmber, fontSize = 17.sp)
                Text(if (lanSearching) "Buscando en la red local..." else "El descubrimiento LAN está listo", color = AuraMuted, fontSize = 13.sp)
                Text("La búsqueda se realiza solo en esta red local. No se comparte ubicación ni datos privados.", color = AuraMuted, fontSize = 12.sp)
                if (lanPeers.isEmpty() && !lanSearching) Text("No se encontraron Auras cercanas", color = AuraAmber, fontSize = 14.sp)
                lanPeers.forEach { peer ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Aura encontrada", color = AuraCyan, fontSize = 13.sp)
                        Text("${peer.name} · ${peer.auraId}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        Text("Nivel: ${peer.guardianLevel}", color = AuraMuted, fontSize = 11.sp)
                        Text("Última señal: ${peer.timestamp}", color = AuraMuted, fontSize = 11.sp)
                    }
                }
                lastLanScan?.let { Text("Última comprobación: $it", color = AuraMuted, fontSize = 11.sp) }
            }
        }
        ActionButton(if (locationActive) "Actualizar permiso de ubicación" else "Activar ubicación", onClick = onActivateLocation)
        ActionButton(if (visible) "Modo visible" else "Modo invisible", onClick = onVisibilityToggle, outlined = true)
        ActionButton(if (lanSearching) "Detener búsqueda" else "Buscar Auras", onClick = if (lanSearching) onStopLanSearch else onSearchLan, outlined = true)
    }
}

@Composable
fun DefenseScreen(onModuleDialog: (String, String) -> Unit, onEmergency: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("DEFENSA", "Cortafuegos Centinela", "Superficie de control visual. No se simulan bloqueos.")
        Panel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SentinelCanvas(Modifier.fillMaxWidth().height(250.dp))
                Text("LISTO / PARCIAL", color = AuraGreen, fontSize = 12.sp)
            }
        }
        ActionButton("Preparar VPN", onClick = { onModuleDialog("Preparar VPN", "El servicio VPN real se implementará en la fase de cortafuegos. Aura no mostrará protección VPN activa hasta que Android confirme el servicio.") })
        ActionButton("Perfiles del cortafuegos", onClick = { onModuleDialog("Perfiles del cortafuegos", "El cortafuegos VPN aún no está implementado. No se aplicarán bloqueos ni se guardarán perfiles activos.") }, outlined = true)
        ActionButton("Modo emergencia", onClick = onEmergency, outlined = true)
    }
}

@Composable
private fun SentinelCanvas(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "sentinel")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(5000), RepeatMode.Restart), label = "orbit")
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.26f
        drawCircle(AuraGreen.copy(alpha = 0.12f), radius * 1.55f, center)
        drawCircle(AuraGreen.copy(alpha = 0.35f), radius, center, style = Stroke(2.dp.toPx()))
        drawCircle(AuraGreen, radius * 0.32f, center)
        listOf(0f, 90f, 180f, 270f).forEachIndexed { index, angle ->
            val radians = Math.toRadians((angle + rotation).toDouble())
            val node = Offset(center.x + kotlin.math.cos(radians).toFloat() * radius * 1.48f, center.y + kotlin.math.sin(radians).toFloat() * radius * 1.48f)
            drawLine(AuraGreen.copy(alpha = 0.45f), center, node, 1.dp.toPx(), StrokeCap.Round)
            drawCircle(if (index == 2) AuraAmber else AuraGreen, 8.dp.toPx(), node)
        }
    }
}

@Composable
fun AppsScreen(
    scanResult: AppScanResult?,
    scanning: Boolean,
    onScan: () -> Unit,
    onViewRisks: () -> Unit,
    onExport: () -> Unit,
    onModuleDialog: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("APPS", "Escáner genómico de apps", "Interfaz visual del escáner. No se inventan resultados del gestor de paquetes.")
        Panel(modifier = Modifier.fillMaxWidth()) { ScannerCanvas(Modifier.fillMaxWidth().height(190.dp), scanning) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("RED", "SENSORES", "IDENTIDAD").forEach { label ->
                Box(modifier = Modifier.weight(1f).background(AuraSurface, RoundedCornerShape(8.dp)).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = AuraMuted, fontSize = 10.sp)
                }
            }
        }
        Panel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("RESUMEN DE RIESGOS", color = AuraCyan, fontSize = 11.sp)
                Text(scanResult?.let { "${it.apps.size} apps visibles por Android" } ?: "Sin escaneo ejecutado", color = AuraMuted, fontSize = 14.sp)
                scanResult?.let {
                    Text("Riesgos: ${it.riskyApps.size} · Riesgo alto: ${it.highRiskApps.size}", color = AuraMuted, fontSize = 13.sp)
                    Text("Último escaneo: ${it.scannedAt}", color = AuraMuted, fontSize = 12.sp)
                }
            }
        }
        ActionButton(if (scanning) "Escaneando apps..." else "Escanear apps", onClick = onScan)
        ActionButton("Ver riesgos", onClick = onViewRisks, outlined = true)
        ActionButton("Exportar reporte de apps", onClick = onExport, outlined = true)
    }
}

@Composable
private fun ScannerCanvas(modifier: Modifier, scanning: Boolean) {
    val transition = rememberInfiniteTransition(label = "scanner")
    val beam by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), if (scanning) RepeatMode.Reverse else RepeatMode.Restart), label = "beam")
    Canvas(modifier) {
        val columns = 6
        val rows = 3
        val cellWidth = size.width / (columns + 1)
        val cellHeight = size.height / (rows + 1)
        repeat(rows) { row ->
            repeat(columns) { column ->
                val point = Offset(cellWidth * (column + 1), cellHeight * (row + 1))
                drawCircle(AuraCyan.copy(alpha = 0.45f), 6.dp.toPx(), point)
            }
        }
        val beamX = size.width * beam
        drawLine(AuraCyan.copy(alpha = 0.8f), Offset(beamX, 0f), Offset(beamX, size.height), 2.dp.toPx())
    }
}
