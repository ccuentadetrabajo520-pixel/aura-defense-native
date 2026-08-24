package com.aura.defense.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraSurfaceRaised

@Composable
fun AuraCenterDialog(
    auraId: String,
    visibilityVisible: Boolean,
    onEditId: () -> Unit,
    onVisibilityToggle: () -> Unit,
    onTelemetry: () -> Unit,
    onItemClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val permissions = listOf("Permiso de VPN", "Permiso de ubicación", "Permiso de cámara", "Permiso de notificaciones", "Acceso a notificaciones", "Acceso al uso", "Optimización de batería")
    val privacy = listOf("Modo local prioritario", "No subir apps", "No subir ubicación", "No subir URLs", "Borrar historial local")
    val tools = listOf("Escáner de apps", "Analizador de enlaces", "Antiphishing QR", "Auditor de contraseñas", "Escáner de contenido compartido", "Protección de notificaciones", "Reportes", "Bóveda cifrada", "Modo emergencia", "Comprobaciones programadas", "Acceso rápido", "Auras LAN")
    val legal = listOf("Términos y condiciones", "Responsabilidad del usuario", "Límites reales de Android sin root")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Centro Aura") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CenterSection("IDENTIDAD") {
                    CenterRow("Aura ID editable", auraId, onEditId)
                    CenterRow("Modo de visibilidad", if (visibilityVisible) "Visible" else "Invisible", onVisibilityToggle)
                }
                CenterRow("Telemetría del dispositivo", "Ver datos", onTelemetry)
                CenterSection("PERMISOS") { permissions.forEach { CenterRow(it, "No conectado", { onItemClick(it) }) } }
                CenterSection("PRIVACIDAD") { privacy.forEach { CenterRow(it, "Ajuste local", { onItemClick(it) }) } }
                CenterSection("HERRAMIENTAS") { tools.forEach { CenterRow(it, "Módulo", { onItemClick(it) }) } }
                CenterSection("LEGAL") { legal.forEach { CenterRow(it, "Información", { onItemClick(it) }) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun CenterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = AuraCyan, fontSize = 11.sp)
        content()
    }
}

@Composable
private fun CenterRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = AuraMuted, fontSize = 11.sp)
    }
}
