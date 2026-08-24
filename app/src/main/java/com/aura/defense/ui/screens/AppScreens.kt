package com.aura.defense.ui.screens

import androidx.compose.animation.core.RepeatMode
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

@Composable
fun HomeScreen(
    auraScore: String,
    vpnStatus: String,
    dnsStatus: String,
    riskCount: Int,
    onStartScan: () -> Unit,
    onModuleDialog: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("HOME DASHBOARD", "Your defense cockpit", "Local UI state only. Native security modules are not connected yet.")
        Panel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("AURA SCORE", color = AuraMuted, fontSize = 11.sp)
                        Text(auraScore, color = AuraCyan, fontSize = 24.sp)
                    }
                    StatusDot(AuraAmber, "STATUS", "Partial")
                }
                RadarCanvas(Modifier.fillMaxWidth().height(150.dp))
            }
        }
        Panel(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Metric("VPN", vpnStatus, AuraAmber)
                Metric("DNS", dnsStatus, AuraGreen)
                Metric("RISKS", riskCount.toString(), AuraRed)
            }
        }
        ActionButton("Start Scan") { onStartScan() }
        ActionButton("Activate VPN Defense", onClick = { onModuleDialog("VPN Defense", "VPN Defense will be connected to VpnService in Phase 4/5.") }, outlined = true)
        ActionButton("Emergency Mode", onClick = { onModuleDialog("Emergency Mode", "Emergency Mode will enable the configured protective response when native defense modules are connected.") }, outlined = true)
    }
}

@Composable
fun AurasScreen(onModuleDialog: (String, String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("AURAS", "Tactical field", "Privacy-safe view with no location or LAN data active.")
        TacticalMap(Modifier.fillMaxWidth().height(250.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Location not enabled", color = AuraAmber, fontSize = 17.sp)
                Text("LAN discovery not active yet", color = AuraMuted, fontSize = 13.sp)
                Text("No nearby Aura data is shown until native permissions and discovery are connected.", color = AuraMuted, fontSize = 12.sp)
            }
        }
        ActionButton("Enable location", onClick = { onModuleDialog("Location permission", "Location will use the Android location permission when the native location module is connected.") })
        ActionButton("Search LAN Auras", onClick = { onModuleDialog("LAN discovery", "LAN Auras will use UDP discovery in a later native phase.") }, outlined = true)
    }
}

@Composable
fun DefenseScreen(onModuleDialog: (String, String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("DEFENSE", "Sentinel Firewall", "A visual control surface. No blocking events are simulated.")
        Panel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SentinelCanvas(Modifier.fillMaxWidth().height(250.dp))
                Text("READY / PARTIAL", color = AuraGreen, fontSize = 12.sp)
            }
        }
        ActionButton("Prepare VPN", onClick = { onModuleDialog("Prepare VPN", "VPN preparation will connect to the native VpnService in a later phase.") })
        ActionButton("Firewall Profiles", onClick = { onModuleDialog("Firewall Profiles", "Firewall profiles will be connected to native Android controls in the next implementation phase.") }, outlined = true)
        ActionButton("Emergency Mode", onClick = { onModuleDialog("Emergency Mode", "Emergency Mode will define its native response when the defense modules are connected.") }, outlined = true)
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
fun AppsScreen(onModuleDialog: (String, String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("APPS", "Apps Genome Scanner", "Visual scanner shell. No PackageManager results are fabricated.")
        Panel(modifier = Modifier.fillMaxWidth()) { ScannerCanvas(Modifier.fillMaxWidth().height(190.dp)) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("NETWORK", "SENSORS", "IDENTITY").forEach { label ->
                Box(modifier = Modifier.weight(1f).background(AuraSurface, RoundedCornerShape(8.dp)).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = AuraMuted, fontSize = 10.sp)
                }
            }
        }
        Panel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("RISK SUMMARY", color = AuraCyan, fontSize = 11.sp)
                Text("Awaiting real PackageManager scan", color = AuraMuted, fontSize = 14.sp)
            }
        }
        ActionButton("Scan visible apps", onClick = { onModuleDialog("Apps scanner", "The real PackageManager scanner will be connected in the next phase.") })
        ActionButton("Permission audit", onClick = { onModuleDialog("Permission audit", "The real PackageManager permission audit will be connected in the next phase.") }, outlined = true)
        ActionButton("Export app report", onClick = { onModuleDialog("App report", "Report export will be connected after the real PackageManager scanner is available.") }, outlined = true)
    }
}

@Composable
private fun ScannerCanvas(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "scanner")
    val beam by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "beam")
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
