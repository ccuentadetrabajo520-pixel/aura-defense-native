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
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.aura.defense.ui.components.Metric
import com.aura.defense.guardian.AuraGuardianAssessment
import com.aura.defense.ui.components.AuraGuardianPanel
import com.aura.defense.util.Haptics
import com.aura.defense.apps.AppScanResult
import com.aura.defense.apps.AppRiskSeverity
import com.aura.defense.apps.InstalledAppInfo
import com.aura.defense.lan.AuraLanPeer
import com.aura.defense.monitor.CorrelationAlert
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
    correlationAlerts: List<CorrelationAlert> = emptyList(),
    historyCount: Int,
    inBackground: Boolean = false,
    onGuardianAnalysis: () -> Unit,
    onStartScan: () -> Unit,
    onModuleDialog: (String, String) -> Unit,
    onEmergency: () -> Unit,
    onToolsHub: () -> Unit
) {
    val blink = if (inBackground) {
        0.4f
    } else {
        rememberInfiniteTransition(label = "hb").animateFloat(
            0.4f,
            1f,
            infiniteRepeatable(tween(1200), RepeatMode.Reverse),
            label = "bl"
        ).value
    }
    val scoreColor = if (result.score >= 85) AuraGreen else if (result.score >= 60) AuraAmber else AuraRed
    val findings = result.findings.take(4)

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        correlationAlerts.forEach { alert ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AuraRed.copy(alpha = 0.14f)),
                border = BorderStroke(0.8.dp, AuraRed.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(AuraSpacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⚠ ${alert.title}", color = AuraRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(alert.detail, color = AuraText, fontSize = 12.sp)
                    TextButton(onClick = { onModuleDialog("Evidencia: ${alert.title}", alert.evidence.joinToString("\n")) }) {
                        Text("¿Por qué?", color = AuraRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
            Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                Text(if (logs.isEmpty()) "Sin bloqueos todavía — eso es buena señal, no significa que no funcione." else logs.takeLast(8).joinToString("\n"), color = if (logs.isEmpty()) AuraMuted else Color(0xFF00FF41), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp)
                Box(modifier = Modifier.size(6.dp).background(scoreColor.copy(alpha = blink), CircleShape))
                Text(guardianAssessment.level.name, color = scoreColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text(guardianAssessment.confidence.name, color = AuraCyan.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
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
                drawCircle(scoreColor.copy(alpha = 0.25f), r, center = Offset(cx, cy), style = Stroke(1.dp.toPx()))
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(if (result.score >= 0) "${result.score}" else "--", color = scoreColor, fontSize = 52.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("/100", color = AuraMuted, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(2.dp))
                Text(result.status, color = scoreColor.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                Triple("VPN", if (result.telemetry.vpnActive) "ACTIVA" else "INACTIVA", if (result.telemetry.vpnActive) AuraGreen else AuraMuted),
                Triple("DNS", result.telemetry.privateDnsStatus, if (result.telemetry.privateDnsStatus.contains("Activo") || result.telemetry.privateDnsStatus.contains("Autom")) AuraGreen else AuraMuted),
                Triple("AMENAZAS", "${findings.count { it.severity >= com.aura.defense.security.FindingSeverity.MEDIUM }}", if (findings.any { it.severity >= com.aura.defense.security.FindingSeverity.MEDIUM }) AuraRed else AuraGreen),
                Triple("HISTORIAL", "$historyCount", AuraCyan.copy(alpha = 0.6f))
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
                Text("ALERTAS", color = AuraRed.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
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
                Pair("ESCANEAR", onStartScan),
                Pair("HERRAMIENTAS", onToolsHub),
                Pair("EMERGENCIA", onEmergency)
            ).forEach { (label, action) ->
                Surface(
                    modifier = Modifier.weight(1f).clickable(onClick = action),
                    shape = RoundedCornerShape(10.dp),
                    color = if (label == "ESCANEAR") AuraCyan.copy(alpha = 0.12f) else AuraSurface,
                    border = BorderStroke(0.5.dp, if (label == "ESCANEAR") AuraCyan.copy(alpha = 0.35f) else AuraCyan.copy(alpha = 0.12f))
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
    val statusColor = if (locationActive) AuraGreen else AuraAmber

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CAMPO TÁCTICO", color = AuraCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(6.dp).background(statusColor.copy(alpha = 0.8f), CircleShape))
                Text(if (locationActive) "UBICACIÓN ACTIVA" else "UBICACIÓN INACTIVA", color = statusColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(AuraSurface, RoundedCornerShape(14.dp)).border(BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.12f)), RoundedCornerShape(14.dp))) {
            HonestTacticalMap(Modifier.fillMaxSize(), lanPeers, lanSearching)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Triple("AURAS", "${lanPeers.size}", AuraCyan),
                Triple("HISTORIAL", "${historyEntries.size}", AuraMuted),
                Triple("ESTADO", if (lanSearching) "ESCANEANDO" else "LISTO", if (lanSearching) AuraAmber else AuraGreen)
            ).forEach { (label, value, color) ->
                Column(modifier = Modifier.weight(1f).background(AuraSurface, RoundedCornerShape(10.dp)).padding(vertical = 10.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, color = AuraMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        if (historyEntries.isEmpty()) {
            Text(
                "Cada diagnóstico que ejecutes se guardará aquí.",
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = AuraMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }

        if (lanPeers.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().background(AuraSurface, RoundedCornerShape(10.dp)).border(BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.12f)), RoundedCornerShape(10.dp)).padding(AuraSpacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("AURAS DETECTADAS", color = AuraCyan.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                lanPeers.forEach { peer ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(peer.name, color = AuraText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${peer.auraId} | ${peer.guardianLevel}", color = AuraMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text(formatDnsTime(peer.timestamp), color = AuraMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Pair(if (locationActive) "ACTUALIZAR UBICACIÓN" else "ACTIVAR UBICACIÓN", onActivateLocation),
                Pair(if (visible) "MODO VISIBLE" else "MODO SIGILO", onVisibilityToggle),
                Pair(if (lanSearching) "DETENER ESCANEO" else "ESCANEAR RED", if (lanSearching) onStopLanSearch else onSearchLan)
            ).forEach { (label, action) ->
                Surface(modifier = Modifier.weight(1f).clickable(onClick = action), shape = RoundedCornerShape(10.dp), color = AuraSurface, border = BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.12f))) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = AuraMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun HonestTacticalMap(modifier: Modifier, peers: List<AuraLanPeer>, searching: Boolean) {
    val sweep by rememberInfiniteTransition(label = "honest-radar").animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(4000, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart),
        label = "honest-sweep"
    )
    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 6.dp.toPx()))
            val gridColor = AuraCyan.copy(alpha = 0.06f)
            val spacing = 34.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 0.5.dp.toPx(), pathEffect = dash)
                x += spacing
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 0.5.dp.toPx(), pathEffect = dash)
                y += spacing
            }
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.42f
            drawCircle(AuraCyan.copy(alpha = 0.08f), radius, center, style = Stroke(1.dp.toPx()))
            val angle = Math.toRadians(sweep.toDouble())
            drawLine(
                AuraCyan.copy(alpha = 0.4f),
                center,
                Offset(center.x + kotlin.math.cos(angle).toFloat() * radius, center.y + kotlin.math.sin(angle).toFloat() * radius),
                1.dp.toPx()
            )
            peers.forEach { peer ->
                val hash = peer.auraId.hashCode()
                val xRatio = 0.12f + Math.floorMod(hash, 76) / 100f
                val yRatio = 0.12f + Math.floorMod(hash / 100, 76) / 100f
                val point = Offset(size.width * xRatio, size.height * yRatio)
                val color = when {
                    peer.guardianLevel.contains("CRITICAL", ignoreCase = true) -> AuraRed
                    peer.guardianLevel.contains("HIGH", ignoreCase = true) -> AuraAmber
                    else -> AuraGreen
                }
                drawCircle(color.copy(alpha = 0.18f), 12.dp.toPx(), point)
                drawCircle(color, 5.dp.toPx(), point)
            }
        }
        if (peers.isEmpty()) {
            Text(
                if (searching) "Escaneando red local..." else "Sin Auras cercanas todavía",
                modifier = Modifier.align(Alignment.Center),
                color = AuraMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
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
    val statusColor = if (vpnStatus == "Protegido") AuraGreen else AuraAmber
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val vpnArc = remember { androidx.compose.animation.core.Animatable(0f) }
    val vpnArcAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val vpnOffFlash = remember { androidx.compose.animation.core.Animatable(0f) }
    var previousVpnRunning by remember { mutableStateOf<Boolean?>(null) }
    var previousBlockPulse by remember { mutableStateOf(blockPulse) }

    LaunchedEffect(vpnRunning, lifecycleState) {
        val wasRunning = previousVpnRunning
        previousVpnRunning = vpnRunning
        if (wasRunning == false && vpnRunning) {
            Haptics.confirm(context)
            if (lifecycleState == Lifecycle.State.RESUMED) {
                vpnArc.snapTo(0f)
                vpnArcAlpha.snapTo(1f)
                vpnArc.animateTo(1f, tween(600))
                vpnArcAlpha.animateTo(0f, tween(300))
            } else {
                vpnArc.snapTo(1f)
                vpnArcAlpha.snapTo(0f)
            }
        } else if (wasRunning == true && !vpnRunning) {
            vpnArc.snapTo(0f)
            vpnArcAlpha.snapTo(0f)
            if (lifecycleState == Lifecycle.State.RESUMED) {
                vpnOffFlash.snapTo(1f)
                vpnOffFlash.animateTo(0f, tween(200))
            }
        } else if (!vpnRunning) {
            vpnArc.snapTo(0f)
            vpnArcAlpha.snapTo(0f)
        }
    }

    LaunchedEffect(blockPulse, lifecycleState) {
        if (blockPulse > previousBlockPulse) Haptics.alert(context)
        previousBlockPulse = blockPulse
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CONTROL DE DEFENSA", color = AuraCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(6.dp).background(statusColor.copy(alpha = 0.8f), CircleShape))
                Text(vpnStatus.uppercase(), color = statusColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp)
                .background(AuraSurface, RoundedCornerShape(14.dp))
                .border(BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.15f)), RoundedCornerShape(14.dp))
        ) {
            SentinelCanvas(Modifier.fillMaxSize(), blockPulse)
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.26f
                val radius = baseRadius * (0.4f + vpnArc.value * 0.6f)
                listOf(0.12f, 0.24f, 0.4f).forEach { alpha ->
                    drawArc(
                        color = AuraCyan.copy(alpha = alpha * vpnArcAlpha.value),
                        startAngle = -90f,
                        sweepAngle = 360f * vpnArc.value,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                        style = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                if (vpnOffFlash.value > 0f) {
                    drawRoundRect(
                        color = AuraRed.copy(alpha = 0.5f * vpnOffFlash.value),
                        style = Stroke(3.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx())
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onVpnToggle),
            shape = RoundedCornerShape(10.dp),
            color = if (vpnRunning) AuraGreen.copy(alpha = 0.12f) else AuraSurface,
            border = BorderStroke(0.5.dp, if (vpnRunning) AuraGreen.copy(alpha = 0.35f) else AuraCyan.copy(alpha = 0.12f))
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                Text(if (vpnRunning) "DESACTIVAR TÚNEL DNS" else "ACTIVAR TÚNEL DNS", color = if (vpnRunning) AuraGreen else AuraCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(AuraSurface, RoundedCornerShape(10.dp))
                .border(BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.12f)), RoundedCornerShape(10.dp))
                .padding(AuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("FIREWALL DNS", color = AuraCyan.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            Text("Perfil activo: ${firewallProfile.label}", color = AuraText, fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DnsFirewallProfile.entries.forEach { profile ->
                    val active = profile == firewallProfile
                    Surface(
                        modifier = Modifier.weight(1f).clickable(onClick = { onProfileChange(profile) }),
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) AuraCyan.copy(alpha = 0.15f) else Color.Transparent,
                        border = BorderStroke(0.5.dp, if (active) AuraCyan.copy(alpha = 0.4f) else AuraCyan.copy(alpha = 0.08f))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(if (active) "${profile.label} ACTIVO" else profile.label, color = if (active) AuraCyan else AuraMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(AuraSurface, RoundedCornerShape(10.dp))
                .border(BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.12f)), RoundedCornerShape(10.dp))
                .padding(AuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DOMINIOS BLOQUEADOS", color = AuraRed.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Text("$blockedDomainCount", color = AuraRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp)
                    .background(AuraBackground, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(if (logs.isEmpty()) "Aún no veo bloqueos DNS. Sigo vigilando." else logs.takeLast(8).joinToString("\n"), color = Color(0xFF00FF41), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onEmergency),
            shape = RoundedCornerShape(10.dp),
            color = AuraRed.copy(alpha = 0.08f),
            border = BorderStroke(0.5.dp, AuraRed.copy(alpha = 0.2f))
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                Text("MODO EMERGENCIA", color = AuraRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            }
        }
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
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ESCÁNER DE APLICACIONES", color = AuraCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            if (scanning) {
                val scanBlink by rememberInfiniteTransition(label = "sb").animateFloat(0.3f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "sb")
                Text("ESCANEANDO", color = AuraAmber.copy(alpha = scanBlink), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(AuraSurface, RoundedCornerShape(14.dp)).border(BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.12f)), RoundedCornerShape(14.dp))) {
            ScannerCanvas(Modifier.fillMaxSize(), scanning)
        }

        scanResult?.let { scan ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    Triple("TOTAL", "${scan.apps.size}", AuraCyan),
                    Triple("RIESGO", "${scan.riskyApps.size}", AuraAmber),
                    Triple("RIESGO ALTO", "${scan.highRiskApps.size}", AuraRed),
                    Triple("ESCANEADAS", scan.scannedAt, AuraMuted)
                ).forEach { (label, value, color) ->
                    Column(modifier = Modifier.weight(1f).background(AuraSurface, RoundedCornerShape(10.dp)).padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, color = AuraMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
                        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1)
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxWidth().background(AuraSurface, RoundedCornerShape(10.dp)).padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aún no escaneé tus aplicaciones. ¿Empezamos?", color = AuraMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                    TextButton(onClick = onScan) { Text("EMPEZAR ESCANEO", color = AuraCyan) }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(modifier = Modifier.weight(1f).clickable(onClick = onScan), shape = RoundedCornerShape(10.dp), color = AuraCyan.copy(alpha = 0.12f), border = BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.35f))) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text(if (scanning) "Escaneando…" else "ESCANEAR APLICACIONES", color = AuraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                }
            }
            Surface(modifier = Modifier.weight(1f).clickable(onClick = onViewRisks), shape = RoundedCornerShape(10.dp), color = AuraSurface, border = BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.12f))) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text("VER RIESGOS", color = AuraMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                }
            }
        }
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

