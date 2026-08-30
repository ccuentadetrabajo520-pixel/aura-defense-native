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
import com.aura.defense.ui.components.Metric
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
    val statusColor = if (locationActive) AuraGreen else AuraAmber

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("TACTICAL FIELD", color = AuraCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(6.dp).background(statusColor.copy(alpha = 0.8f), CircleShape))
                Text(if (locationActive) "LOC ON" else "LOC OFF", color = statusColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(AuraSurface, RoundedCornerShape(14.dp)).border(BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.12f)), RoundedCornerShape(14.dp))) {
            TacticalMap(Modifier.fillMaxSize())
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Triple("PEERS", "${lanPeers.size}", AuraCyan),
                Triple("HISTORY", "$historyCount", AuraMuted),
                Triple("STATUS", if (lanSearching) "SCANNING" else "READY", if (lanSearching) AuraAmber else AuraGreen)
            ).forEach { (label, value, color) ->
                Column(modifier = Modifier.weight(1f).background(AuraSurface, RoundedCornerShape(10.dp)).padding(vertical = 10.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, color = AuraMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        if (lanPeers.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().background(AuraSurface, RoundedCornerShape(10.dp)).border(BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.12f)), RoundedCornerShape(10.dp)).padding(AuraSpacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("DISCOVERED AURAS", color = AuraCyan.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                lanPeers.forEach { peer ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(peer.name, color = AuraText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${peer.auraId} | ${peer.guardianLevel}", color = AuraMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text(peer.timestamp, color = AuraMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Pair(if (locationActive) "UPDATE LOCATION" else "ENABLE LOCATION", onActivateLocation),
                Pair(if (visible) "VISIBLE MODE" else "STEALTH MODE", onVisibilityToggle),
                Pair(if (lanSearching) "STOP SCAN" else "SCAN NETWORK", if (lanSearching) onStopLanSearch else onSearchLan)
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

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DEFENSE CONTROL", color = AuraCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
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
        }

        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onVpnToggle),
            shape = RoundedCornerShape(10.dp),
            color = if (vpnRunning) AuraGreen.copy(alpha = 0.12f) else AuraSurface,
            border = BorderStroke(0.5.dp, if (vpnRunning) AuraGreen.copy(alpha = 0.35f) else AuraCyan.copy(alpha = 0.12f))
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                Text(if (vpnRunning) "DISABLE VPN TUNNEL" else "ACTIVATE VPN TUNNEL", color = if (vpnRunning) AuraGreen else AuraCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(AuraSurface, RoundedCornerShape(10.dp))
                .border(BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.12f)), RoundedCornerShape(10.dp))
                .padding(AuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("DNS FIREWALL", color = AuraCyan.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            Text("Active profile: ${firewallProfile.label}", color = AuraText, fontSize = 13.sp)
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
                            Text(if (active) "${profile.label} ACTIVE" else profile.label, color = if (active) AuraCyan else AuraMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp, maxLines = 1)
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
                Text("BLOCKED DOMAINS", color = AuraRed.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Text("$blockedDomainCount", color = AuraRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp)
                    .background(AuraBackground, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(if (logs.isEmpty()) "Awaiting DNS queries..." else logs.takeLast(8).joinToString("\n"), color = Color(0xFF00FF41), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onEmergency),
            shape = RoundedCornerShape(10.dp),
            color = AuraRed.copy(alpha = 0.08f),
            border = BorderStroke(0.5.dp, AuraRed.copy(alpha = 0.2f))
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                Text("EMERGENCY MODE", color = AuraRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
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
            Text("APP GENOME SCANNER", color = AuraCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            if (scanning) {
                val scanBlink by rememberInfiniteTransition(label = "sb").animateFloat(0.3f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "sb")
                Text("SCANNING", color = AuraAmber.copy(alpha = scanBlink), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(AuraSurface, RoundedCornerShape(14.dp)).border(BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.12f)), RoundedCornerShape(14.dp))) {
            ScannerCanvas(Modifier.fillMaxSize(), scanning)
        }

        scanResult?.let { scan ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    Triple("TOTAL", "${scan.apps.size}", AuraCyan),
                    Triple("RISKY", "${scan.riskyApps.size}", AuraAmber),
                    Triple("HIGH RISK", "${scan.highRiskApps.size}", AuraRed),
                    Triple("SCANNED", scan.scannedAt, AuraMuted)
                ).forEach { (label, value, color) ->
                    Column(modifier = Modifier.weight(1f).background(AuraSurface, RoundedCornerShape(10.dp)).padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, color = AuraMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
                        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1)
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxWidth().background(AuraSurface, RoundedCornerShape(10.dp)).padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Text("No scan executed", color = AuraMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(modifier = Modifier.weight(1f).clickable(onClick = onScan), shape = RoundedCornerShape(10.dp), color = AuraCyan.copy(alpha = 0.12f), border = BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.35f))) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text(if (scanning) "SCANNING..." else "SCAN APPS", color = AuraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                }
            }
            Surface(modifier = Modifier.weight(1f).clickable(onClick = onViewRisks), shape = RoundedCornerShape(10.dp), color = AuraSurface, border = BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.12f))) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text("VIEW RISKS", color = AuraMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
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

