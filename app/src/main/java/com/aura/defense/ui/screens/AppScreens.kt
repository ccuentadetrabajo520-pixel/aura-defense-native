package com.aura.defense.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraText
import com.aura.defense.ui.AuraBackground
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraRed
import com.aura.defense.ui.AuraSpacing
import com.aura.defense.ui.AuraSurface
import com.aura.defense.ui.AuraSurfaceRaised
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
import com.aura.defense.vpn.DnsBlockedEvent
import com.aura.defense.vpn.DnsFirewallProfile
import com.aura.defense.vpn.VpnDebugger
import com.aura.defense.vpn.ProfileManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    result: com.aura.defense.security.PostureResult,
    guardianAssessment: AuraGuardianAssessment,
    historyCount: Int,
    onGuardianAnalysis: () -> Unit,
    onStartScan: () -> Unit,
    onModuleDialog: (String, String) -> Unit,
    onEmergency: () -> Unit,
    onToolsHub: () -> Unit
) {
    val blink by rememberInfiniteTransition(label = "hb").animateFloat(0.4f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "bl")
    val scoreColor = if (result.score >= 85) AuraGreen else if (result.score >= 60) AuraAmber else AuraRed
    val findings = result.findings.take(4)

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(6.dp).background(scoreColor.copy(alpha = blink), CircleShape))
                Text(guardianAssessment.level.displayName, color = scoreColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text("AURA $guardianAssessment.score", color = AuraCyan.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(140.dp)
                .background(AuraSurface, RoundedCornerShape(14.dp))
                .border(BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.15f)), RoundedCornerShape(14.dp))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension * 0.38f
                drawCircle(scoreColor.copy(alpha = 0.06f), r * 1.4f, center = Offset(cx, cy))
                drawCircle(Color.Transparent, r, center = Offset(cx, cy), style = Stroke(1.dp.toPx(), color = scoreColor.copy(alpha = 0.25f)))
                val sides = 6
                for (i in 0 until sides) {
                    val a1 = Math.toRadians((60.0 * i - 90.0))
                    val a2 = Math.toRadians((60.0 * ((i + 1) % sides) - 90.0))
                    val x1 = cx + r * kotlin.math.cos(a1).toFloat()
                    val y1 = cy + r * kotlin.math.sin(a1).toFloat()
                    val x2 = cx + r * kotlin.math.cos(a2).toFloat()
                    val y2 = cy + r * kotlin.math.sin(a2).toFloat()
                    drawLine(scoreColor.copy(alpha = 0.3f), Offset(x1, y1), Offset(x2, y2), 1.dp.toPx())
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(AuraSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(if (result.score >= 0) "${result.score}" else "--", color = scoreColor, fontSize = 52.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("/100", color = AuraMuted, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(2.dp))
                Text(result.status, color = scoreColor.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                Triple("VPN", if (result.telemetry.vpnActive) "ON" else "OFF", if (result.telemetry.vpnActive) AuraGreen else AuraMuted),
                Triple("DNS", result.telemetry.privateDnsStatus, if (result.telemetry.privateDnsStatus.contains("Activo") || result.telemetry.privateDnsStatus.contains("Autom")) AuraGreen else AuraMuted),
                Triple("THREATS", "${findings.count { it.severity >= com.aura.defense.security.FindingSeverity.MEDIUM }}", if (findings.any { it.severity >= com.aura.defense.security.FindingSeverity.MEDIUM }) AuraRed else AuraGreen),
                Triple("HISTORY", "$historyCount", AuraCyan.copy(alpha = 0.6f))
            ).forEach { (label, value, color) ->
                Column(
                    modifier = Modifier.weight(1f).background(AuraSurface, RoundedCornerShape(10.dp)).padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(label, color = AuraMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1)
                }
            }
        }

        if (findings.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(AuraSurface, RoundedCornerShape(10.dp))
                    .border(BorderStroke(0.5.dp, AuraRed.copy(alpha = 0.15f)), RoundedCornerShape(10.dp))
                    .padding(AuraSpacing.md)
            ) {
                Text("THREAT FEED", color = AuraRed.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                findings.forEach { finding ->
                    val sevColor = when (finding.severity) {
                        com.aura.defense.security.FindingSeverity.HIGH -> AuraRed
                        com.aura.defense.security.FindingSeverity.MEDIUM -> AuraAmber
                        else -> AuraMuted
                    }
                    val sevTag = when (finding.severity) {
                        com.aura.defense.security.FindingSeverity.HIGH -> "HI "
                        com.aura.defense.security.FindingSeverity.MEDIUM -> "MED"
                        else -> "LOW"
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("[$sevTag]", color = sevColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(48.dp))
                        Text(finding.title, color = AuraText, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Pair("SCAN", onStartScan),
                Pair("TOOLS", onToolsHub),
                Pair("EMERGENCY", onEmergency)
            ).forEach { (label, action) ->
                Surface(
                    modifier = Modifier.weight(1f).clickable(onClick = action),
                    shape = RoundedCornerShape(10.dp),
                    color = if (label == "SCAN") AuraCyan.copy(alpha = 0.12f) else AuraSurface,
                    border = BorderStroke(0.5.dp, if (label == "SCAN") AuraCyan.copy(alpha = 0.35f) else AuraCyan.copy(alpha = 0.12f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = if (label == "SCAN") AuraCyan else AuraMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AurasScreen(
    locationActive: Boolean,
    lanSearching: Boolean,
    lanPeers: List<AuraLanPeer>,
    lastLanScan: String?,
    historyEntries: List<com.aura.defense.history.AuraHistoryEntry> = emptyList(),
    visible: Boolean,
    onActivateLocation: () -> Unit,
    onVisibilityToggle: () -> Unit,
    onSearchLan: () -> Unit,
    onStopLanSearch: () -> Unit,
    onModuleDialog: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AuraSpacing.lg)) {
        SectionTitle("AURAS", "Campo táctico", if (locationActive) "Ubicación activa. No se muestran coordenadas." else "Vista privada sin ubicación ni datos LAN activos.")
        TacticalMap(Modifier.fillMaxWidth().height(250.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(AuraSpacing.xl), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (locationActive) "Ubicación activa" else "Ubicación no activada", color = if (locationActive) AuraGreen else AuraAmber, fontSize = 17.sp)
                Text(if (lanSearching) "Buscando en la red local..." else "El descubrimiento LAN está listo", color = AuraMuted, fontSize = 13.sp)
                Text("La búsqueda se realiza solo en esta red local. No se comparte ubicación ni datos privados.", color = AuraMuted, fontSize = 12.sp)
                if (lanPeers.isEmpty() && !lanSearching) Text("No se encontraron Auras cercanas", color = AuraAmber, fontSize = 14.sp)
                lanPeers.forEach { peer ->
                    Panel {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Aura encontrada", color = AuraCyan, fontSize = 13.sp)
                            Text("${peer.name} · ${peer.auraId}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            Text("Nivel: ${peer.guardianLevel}", color = AuraMuted, fontSize = 11.sp)
                            Text("Última señal: ${peer.timestamp}", color = AuraMuted, fontSize = 11.sp)
                        }
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
fun DefenseScreen(
    vpnStatus: String,
    vpnRunning: Boolean,
    firewallProfile: DnsFirewallProfile,
    blockedDomains: List<DnsBlockedEvent>,
    blockedDomainCount: Int,
    allowlistedDomains: List<String>,
    blockedManuallyDomains: List<String>,
    blockPulse: Int,
    onProfileChange: (DnsFirewallProfile) -> Unit,
    onAllowlistAdd: (String) -> Unit,
    onAllowlistRemove: (String) -> Unit,
    onBlocklistAdd: (String) -> Unit,
    onBlocklistRemove: (String) -> Unit,
    onVpnToggle: () -> Unit,
    onModuleDialog: (String, String) -> Unit,
    onEmergency: () -> Unit
) {
    val logs by VpnDebugger.logs.collectAsState()
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AuraSpacing.lg)) {
        SectionTitle("DEFENSA", "Cortafuegos Centinela", "Superficie de control visual. No se simulan bloqueos.")
        Panel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(AuraSpacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
                SentinelCanvas(Modifier.fillMaxWidth().height(250.dp), blockPulse)
                Text(vpnStatus, color = if (vpnStatus == "Protegido") AuraGreen else AuraAmber, fontSize = 12.sp)
            }
        }
        ActionButton(if (vpnRunning) "Desactivar VPN" else "Activar defensa VPN", onClick = onVpnToggle)
        Panel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(AuraSpacing.xl), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CORTAFUEGOS DNS", color = AuraCyan, fontSize = 11.sp)
                Text("Perfil: ${firewallProfile.label}", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                DnsFirewallProfile.entries.forEach { profile ->
                    Surface(onClick = { onProfileChange(profile) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = if (profile == firewallProfile) AuraCyan.copy(alpha = 0.12f) else AuraSurface, border = BorderStroke(if (profile == firewallProfile) 1.dp else 0.5.dp, AuraCyan.copy(alpha = if (profile == firewallProfile) 0.4f else 0.1f))) {
                        Text(if (profile == firewallProfile) "${profile.label} · Activo" else profile.label, modifier = Modifier.padding(vertical = 12.dp), color = if (profile == firewallProfile) AuraCyan else AuraMuted, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Text("Dominios bloqueados en esta sesión: $blockedDomainCount", color = if (blockedDomainCount == 0) AuraMuted else AuraAmber, fontSize = 14.sp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(AuraBackground)
                        .border(BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.15f)), RoundedCornerShape(8.dp))
                        .padding(AuraSpacing.sm),
                ) {
                    Text(logs.joinToString("\n"), color = Color(0xFF00FF41), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                blockedDomains.takeLast(5).asReversed().forEach { event ->
                    Text(
                        "${event.domain} · ${event.category} · ${event.severity} · ${formatDnsTime(event.timestamp)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }
                if (blockedDomains.isEmpty()) Text("Todavía no se han bloqueado dominios.", color = AuraMuted, fontSize = 12.sp)
                if (!ProfileManager.isFamilyModeEnabled) {
                    var allowlistInput by remember { mutableStateOf("") }
                    OutlinedTextField(
                    value = allowlistInput,
                    onValueChange = { allowlistInput = it },
                    singleLine = true,
                    label = { Text("Dominio permitido") },
                    modifier = Modifier.fillMaxWidth()
                )
                    TextButton(onClick = {
                    onAllowlistAdd(allowlistInput)
                    allowlistInput = ""
                    }) { Text("Añadir a la lista de permitidos") }
                    allowlistedDomains.forEach { domain ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(domain, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            TextButton(onClick = { onAllowlistRemove(domain) }) { Text("Quitar") }
                        }
                    }
                    var blocklistInput by remember { mutableStateOf("") }
                    OutlinedTextField(
                    value = blocklistInput,
                    onValueChange = { blocklistInput = it },
                    singleLine = true,
                    label = { Text("Dominio bloqueado") },
                    modifier = Modifier.fillMaxWidth()
                )
                    TextButton(onClick = {
                    onBlocklistAdd(blocklistInput)
                    blocklistInput = ""
                    }) { Text("Añadir a la lista de bloqueados") }
                    blockedManuallyDomains.forEach { domain ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(domain, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            TextButton(onClick = { onBlocklistRemove(domain) }) { Text("Quitar") }
                        }
                    }
                }
            }
        }
        ActionButton("Modo emergencia", onClick = onEmergency, outlined = true)
    }
}

@Composable
private fun SentinelCanvas(modifier: Modifier, blockPulse: Int) {
    var pulseActive by remember { mutableStateOf(false) }
    LaunchedEffect(blockPulse) {
        if (blockPulse > 0) {
            pulseActive = true
            kotlinx.coroutines.delay(350)
            pulseActive = false
        }
    }
    val pulse by animateFloatAsState(if (pulseActive) 1f else 0f, tween(350), label = "bloqueo")
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * (0.26f + pulse * 0.04f)
        drawCircle(AuraGreen.copy(alpha = 0.12f + pulse * 0.16f), radius * (1.55f + pulse * 0.2f), center)
        drawCircle(AuraGreen.copy(alpha = 0.35f), radius, center, style = Stroke(2.dp.toPx()))
        drawCircle(AuraGreen, radius * 0.32f, center)
        listOf(0f, 90f, 180f, 270f).forEachIndexed { index, angle ->
            val radians = Math.toRadians(angle.toDouble())
            val node = Offset(center.x + kotlin.math.cos(radians).toFloat() * radius * 1.48f, center.y + kotlin.math.sin(radians).toFloat() * radius * 1.48f)
            drawLine(AuraGreen.copy(alpha = 0.45f), center, node, 1.dp.toPx(), StrokeCap.Round)
            drawCircle(if (index == 2) AuraAmber else AuraGreen, 8.dp.toPx(), node)
        }
    }
}

private fun formatDnsTime(timestamp: Long): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

@Composable
fun AppsScreen(
    scanResult: AppScanResult?,
    scanning: Boolean,
    onScan: () -> Unit,
    onViewRisks: () -> Unit,
    onExport: () -> Unit,
    onModuleDialog: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AuraSpacing.lg)) {
        SectionTitle("APPS", "Escáner genómico de apps", "Interfaz visual del escáner. No se inventan resultados del gestor de paquetes.")
        Panel(modifier = Modifier.fillMaxWidth()) { ScannerCanvas(Modifier.fillMaxWidth().height(190.dp), scanning) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("RED", "SENSORES", "IDENTIDAD").forEach { label ->
                Box(modifier = Modifier.weight(1f).background(if (true) AuraCyan.copy(alpha = 0.08f) else AuraSurface, RoundedCornerShape(12.dp)).border(BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.2f)), RoundedCornerShape(12.dp)).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = AuraMuted, fontSize = 10.sp)
                }
            }
        }
        Panel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(AuraSpacing.xl), verticalArrangement = Arrangement.spacedBy(5.dp)) {
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

@Composable
private fun RowScope.QuickAction(label: String, icon: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        color = AuraSurfaceRaised,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.15f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 20.sp)
            Text(label, color = AuraCyan, fontSize = 10.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}
