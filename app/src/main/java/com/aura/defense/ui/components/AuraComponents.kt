package com.aura.defense.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraSpacing
import com.aura.defense.ui.AuraSurface
import com.aura.defense.ui.AuraSurfaceRaised
import com.aura.defense.util.formatBytes

@Composable
fun AuraTopBar(
    auraId: String,
    onAuraIdClick: () -> Unit,
    onSettingsClick: () -> Unit,
    familyModeEnabled: Boolean,
    cellularDataBlocked: Boolean,
    onFamilyModeToggle: () -> Unit,
    onCellularDataToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("AURA / DEFENSA", color = AuraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Defensa móvil privada", color = AuraMuted, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Familia", color = AuraMuted, fontSize = 11.sp)
                Switch(checked = familyModeEnabled, onCheckedChange = { onFamilyModeToggle() })
            }
            if (!familyModeEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bloquear móvil", color = AuraMuted, fontSize = 11.sp)
                    Switch(checked = cellularDataBlocked, onCheckedChange = { onCellularDataToggle() })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
                modifier = Modifier.clickable(onClick = onAuraIdClick),
                shape = RoundedCornerShape(50),
                color = AuraSurfaceRaised,
                border = BorderStroke(1.dp, AuraCyan.copy(alpha = 0.55f))
            ) {
                Text(auraId, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = AuraCyan, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(44.dp)) {
                Text("⚙", color = AuraCyan, fontSize = 20.sp, maxLines = 1)
            }
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
    Surface(
        modifier = modifier,
        color = AuraSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.1f))
    ) {
        Box {
            content()
            Canvas(Modifier.fillMaxSize().align(Alignment.TopStart)) {
                drawRect(
                    Brush.verticalGradient(listOf(AuraCyan.copy(alpha = 0.06f), Color.Transparent)),
                    size = Size(size.width, 1.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun SectionTitle(eyebrow: String, title: String, detail: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(eyebrow, color = AuraCyan, style = MaterialTheme.typography.labelSmall)
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (detail != null) Text(detail, color = AuraMuted, style = MaterialTheme.typography.bodySmall)
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
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.15f)),
        modifier = Modifier.padding(AuraSpacing.sm)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, color = AuraMuted, style = MaterialTheme.typography.bodySmall)
            Text(value, color = color, style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
fun ActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, outlined: Boolean = false) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(50.dp).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, AuraCyan.copy(alpha = 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraCyan)
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(50.dp).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AuraCyan.copy(alpha = 0.12f),
                contentColor = AuraCyan
            ),
            border = BorderStroke(0.5.dp, AuraCyan.copy(alpha = 0.35f))
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ModuleDialog(title: String, message: String, onDismiss: () -> Unit) {
    AuraHudDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(message) }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}

@Composable
fun AuraIdDialog(currentId: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(currentId) }
    AuraHudDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Aura ID") },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true, label = { Text("Aura ID") }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) { onSave(value); onDismiss() } }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun TelemetryDialog(
    telemetry: com.aura.defense.data.DeviceTelemetrySnapshot,
    onSettingsAction: (com.aura.defense.security.SettingsAction) -> Unit,
    onDismiss: () -> Unit
) {
    AuraHudDialog(
        onDismissRequest = onDismiss,
        title = { Text("Telemetría del dispositivo") },
        text = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryLine("Fabricante", telemetry.manufacturer)
                TelemetryLine("Modelo", telemetry.model)
                TelemetryLine("Android", "${telemetry.androidVersion} (API ${telemetry.apiLevel})")
                TelemetryLine("Parche de seguridad", telemetry.securityPatch)
                TelemetryLine("Batería", telemetry.batteryLevel)
                TelemetryLine("RAM disponible / total", "${formatBytes(telemetry.ramAvailableBytes)} / ${formatBytes(telemetry.ramTotalBytes)}")
                TelemetryLine("Almacenamiento disponible / total", "${formatBytes(telemetry.storageAvailableBytes)} / ${formatBytes(telemetry.storageTotalBytes)}")
                TelemetryLine("Red activa", telemetry.networkActive)
                TelemetryLine("VPN", if (telemetry.vpnActive) "Activa" else "Inactiva")
                TelemetryLine("DNS privado", telemetry.privateDnsStatus)
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
    AuraHudDialog(
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
fun RadarCanvas(score: Int, modifier: Modifier = Modifier) {
    val blink = rememberInfiniteTransition(label = "blink").animateFloat(
        0.3f,
        1f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "b"
    )
    val scoreColor = if (score >= 85) com.aura.defense.ui.AuraGreen else if (score >= 60) AuraAmber else com.aura.defense.ui.AuraRed
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val br = size.minDimension * 0.42f
        listOf(0.4f, 0.7f, 1f).zip(listOf(0.16f, 0.1f, 0.06f)).forEach { (f, a) ->
            drawCircle(scoreColor.copy(alpha = a), br * f, c, style = Stroke(0.8.dp.toPx()))
        }
        drawLine(AuraCyan.copy(alpha = 0.06f), Offset(c.x - br, c.y), Offset(c.x + br, c.y), 0.5.dp.toPx())
        drawLine(AuraCyan.copy(alpha = 0.06f), Offset(c.x, c.y - br), Offset(c.x, c.y + br), 0.5.dp.toPx())
        drawCircle(scoreColor.copy(alpha = 0.15f), br * 0.18f, c)
        drawCircle(scoreColor.copy(alpha = blink.value * 0.9f), br * 0.08f, c)
        listOf(
            Offset(c.x + br * 0.55f, c.y - br * 0.3f),
            Offset(c.x - br * 0.4f, c.y + br * 0.5f),
            Offset(c.x + br * 0.2f, c.y + br * 0.6f)
        ).forEach { point ->
            drawCircle(AuraCyan.copy(alpha = blink.value * 0.4f), 2.dp.toPx(), point)
        }
    }
}

@Composable
fun TacticalMap(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "tp").animateFloat(
        0.3f,
        1f,
        infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "t"
    )
    Canvas(modifier) {
        val sp = 34.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 6.dp.toPx()))
        val gc = AuraCyan.copy(alpha = 0.06f)
        var x = 0f
        while (x < size.width) {
            drawLine(gc, Offset(x, 0f), Offset(x, size.height), 0.5.dp.toPx(), pathEffect = dash)
            x += sp
        }
        var y = 0f
        while (y < size.height) {
            drawLine(gc, Offset(0f, y), Offset(size.width, y), 0.5.dp.toPx(), pathEffect = dash)
            y += sp
        }
        listOf(
            Offset(size.width * 0.3f, size.height * 0.4f),
            Offset(size.width * 0.7f, size.height * 0.3f),
            Offset(size.width * 0.5f, size.height * 0.7f)
        ).forEach { point ->
            drawCircle(AuraGreen.copy(alpha = pulse.value * 0.5f), 5.dp.toPx(), point)
            drawCircle(AuraGreen.copy(alpha = pulse.value * 0.15f), 12.dp.toPx(), point)
        }
    }
}

@Composable
fun AuraToolsHubDialog(onDismiss: () -> Unit) {
    AuraHudDialog(
        onDismissRequest = onDismiss,
        title = { Text("Herramientas Avanzadas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Verificar Integridad del Dispositivo", color = AuraCyan, fontSize = 13.sp)
                Text("Certificado de Seguridad Aura", color = AuraCyan, fontSize = 13.sp)
                Text("Verificar Contrasenas (HaveIBeenPwned)", color = AuraCyan, fontSize = 13.sp)
                Text("Auditoria de Permisos de Apps", color = AuraCyan, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
