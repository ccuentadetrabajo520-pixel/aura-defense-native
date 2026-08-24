package com.aura.defense.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraSurface
import com.aura.defense.ui.AuraSurfaceRaised
import com.aura.defense.util.formatBytes

@Composable
fun AuraTopBar(auraId: String, onAuraIdClick: () -> Unit, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("AURA / DEFENSA", color = AuraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Defensa móvil privada", color = AuraMuted, fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.clickable(onClick = onAuraIdClick),
                shape = RoundedCornerShape(50),
                color = AuraSurfaceRaised,
                border = BorderStroke(1.dp, AuraCyan.copy(alpha = 0.55f))
            ) {
                Text(auraId, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = AuraCyan, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.width(64.dp)) {
                Text("Ajustes", color = AuraMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
fun AuraBottomNav(selected: Int, onSelected: (Int) -> Unit) {
    val labels = listOf("Inicio", "Auras", "Defensa", "Apps")
    Surface(color = AuraSurface, shape = RoundedCornerShape(18.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, label ->
                val active = selected == index
                Surface(
                    modifier = Modifier.weight(1f).clickable { onSelected(index) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) AuraCyan.copy(alpha = 0.16f) else Color.Transparent
                ) {
                    Text(label, modifier = Modifier.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = if (active) AuraCyan else AuraMuted, fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier, color = AuraSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))) {
        content()
    }
}

@Composable
fun SectionTitle(eyebrow: String, title: String, detail: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(eyebrow, color = AuraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        detail?.let { Text(it, color = AuraMuted, fontSize = 13.sp) }
    }
}

@Composable
fun StatusDot(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Column {
            Text(label, color = AuraMuted, fontSize = 11.sp)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun Metric(label: String, value: String, color: Color = AuraCyan) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = AuraMuted, fontSize = 11.sp)
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActionButton(label: String, onClick: () -> Unit, outlined: Boolean = false) {
    if (outlined) {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(10.dp)) { Text(label) }
    } else {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = AuraCyan)) { Text(label, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun ModuleDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(message) }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}

@Composable
fun AuraIdDialog(currentId: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(currentId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Aura ID") },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true, label = { Text("Aura ID") }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) { onSave(value); onDismiss() } }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun TelemetryDialog(
    telemetry: com.aura.defense.data.DeviceTelemetry,
    onSettingsAction: (com.aura.defense.security.SettingsAction) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Telemetría del dispositivo") },
        text = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryLine("Fabricante", telemetry.fabricante)
                TelemetryLine("Modelo", telemetry.modelo)
                TelemetryLine("Android", "${telemetry.versionAndroid} (API ${telemetry.api})")
                TelemetryLine("Parche de seguridad", telemetry.parcheSeguridad)
                TelemetryLine("Batería", telemetry.bateriaPorcentaje?.let { "$it%" } ?: "No disponible")
                TelemetryLine("RAM disponible / total", "${formatBytes(telemetry.ramDisponibleBytes ?: 0L)} / ${formatBytes(telemetry.ramTotalBytes ?: 0L)}")
                TelemetryLine("Almacenamiento disponible / total", "${formatBytes(telemetry.almacenamientoDisponibleBytes ?: 0L)} / ${formatBytes(telemetry.almacenamientoTotalBytes ?: 0L)}")
                TelemetryLine("Red activa", if (telemetry.redActiva) "Sí" else "No")
                TelemetryLine("VPN", if (telemetry.vpnActiva) "Activa" else "Inactiva")
                TelemetryLine("DNS privado", telemetry.dnsPrivado)
                Spacer(Modifier.height(4.dp))
                Text("Accesos de Android", color = AuraCyan, fontSize = 11.sp)
                TextButton(onClick = { onSettingsAction(com.aura.defense.security.SettingsAction.VPN) }) { Text("Abrir ajustes de VPN") }
                TextButton(onClick = { onSettingsAction(com.aura.defense.security.SettingsAction.RED) }) { Text("Abrir ajustes de red") }
                TextButton(onClick = { onSettingsAction(com.aura.defense.security.SettingsAction.DESARROLLADOR) }) { Text("Abrir opciones de desarrollador") }
                TextButton(onClick = { onSettingsAction(com.aura.defense.security.SettingsAction.SEGURIDAD) }) { Text("Abrir ajustes de seguridad") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun TelemetryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AuraMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
    }
}

@Composable
fun PostureSummaryDialog(result: com.aura.defense.security.PostureResult, onFindingAction: (com.aura.defense.security.SecurityFinding) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Diagnóstico real") },
        text = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Puntuación Aura: ${result.score}/100", color = AuraCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Estado: ${result.status}", color = MaterialTheme.colorScheme.onSurface)
                Text("Hora: ${result.timestamp}", color = AuraMuted, fontSize = 12.sp)
                Text("Hallazgos principales", color = AuraCyan, fontSize = 12.sp)
                result.findings.take(3).forEach { finding ->
                    Column(modifier = Modifier.fillMaxWidth().clickable { onFindingAction(finding) }.padding(vertical = 5.dp)) {
                        Text(finding.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        Text(finding.evidence, color = AuraMuted, fontSize = 12.sp)
                    }
                }
                if (result.findings.isEmpty()) Text("No se han detectado señales de riesgo en las comprobaciones disponibles.", color = AuraMuted, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
fun RadarCanvas(modifier: Modifier = Modifier, score: Int = 60) {
    val pulse = remember { androidx.compose.animation.core.Animatable(0.82f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        pulse.animateTo(1.12f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1600), androidx.compose.animation.core.RepeatMode.Reverse))
    }
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.36f
        listOf(0.38f, 0.68f, 1f).forEach { factor -> drawCircle(AuraCyan.copy(alpha = 0.16f), radius * factor, center, style = Stroke(1.dp.toPx())) }
        drawLine(AuraCyan.copy(alpha = 0.25f), Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth = 1.dp.toPx())
        drawLine(AuraCyan.copy(alpha = 0.25f), Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth = 1.dp.toPx())
        val tone = if (score >= 85) com.aura.defense.ui.AuraGreen else if (score >= 60) AuraAmber else com.aura.defense.ui.AuraRed
        drawCircle(tone.copy(alpha = 0.16f), radius * pulse.value, center, style = Stroke(2.dp.toPx()))
        drawCircle(tone, radius * 0.11f, center)
    }
}

@Composable
fun TacticalMap(modifier: Modifier = Modifier) {
    Canvas(modifier.border(1.dp, AuraCyan.copy(alpha = 0.18f), RoundedCornerShape(16.dp))) {
        val step = 34.dp.toPx()
        var x = 0f
        while (x < size.width) { drawLine(Color(0xFF1E4A50).copy(alpha = 0.45f), Offset(x, 0f), Offset(x, size.height)); x += step }
        var y = 0f
        while (y < size.height) { drawLine(Color(0xFF1E4A50).copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y)); y += step }
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(AuraCyan.copy(alpha = 0.12f), 58.dp.toPx(), center)
        drawCircle(AuraCyan, 8.dp.toPx(), center)
        drawCircle(AuraCyan.copy(alpha = 0.65f), 16.dp.toPx(), center, style = Stroke(1.dp.toPx()))
    }
}
