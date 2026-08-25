package com.aura.defense.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    onDismiss: () -> Unit,
    notificationGuardStatus: String = "Desactivado · Acceso requerido",
    permissionStatus: (String) -> String = { "No disponible" }
) {
    val permissions = listOf("Permiso de VPN", "Permiso de ubicación", "Permiso de cámara", "Permiso de notificaciones", "Acceso a notificaciones", "Acceso al uso", "Optimización de batería")
    val privacy = listOf("Modo local prioritario", "No subir apps", "No subir ubicación", "No subir URLs", "Borrar historial local")
    val tools = listOf("Escáner de apps", "Analizador de enlaces", "QR Anti-Phishing", "Auditor de contraseñas", "Escáner de contenido compartido", "Protección de notificaciones", "Inteligencia de amenazas", "Guardián Aura", "Analizador de archivos", "Reportes", "Bóveda cifrada", "Modo emergencia", "Comprobaciones programadas", "Tile de acceso rápido", "Auras LAN")
    val legal = listOf("Términos y condiciones", "Responsabilidad del usuario", "Límites reales de Android sin root")

    AuraHudDialog(
        onDismissRequest = onDismiss,
        title = { Text("Centro Aura") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AuraHudSection("IDENTIDAD") {
                    CenterRow("Aura ID editable", auraId, onEditId)
                    CenterRow("Modo de visibilidad", if (visibilityVisible) "Visible" else "Invisible", onVisibilityToggle)
                }
                CenterRow("Telemetría del dispositivo", "Ver datos", onTelemetry)
                AuraHudSection("PERMISOS") { permissions.forEach { CenterRow(it, permissionStatus(it), { onItemClick(it) }) } }
                AuraHudSection("PRIVACIDAD") { privacy.forEach { CenterRow(it, "Local", { onItemClick(it) }) } }
                AuraHudSection("HERRAMIENTAS") {
                    tools.forEach {
                        CenterRow(it, when (it) {
                            "Protección de notificaciones" -> notificationGuardStatus
                            "Bóveda cifrada" -> "Local"
                            "Comprobaciones programadas" -> "Pendiente"
                            "Tile de acceso rápido" -> "Disponible"
                            else -> "Disponible"
                        }, { onItemClick(it) })
                    }
                }
                AuraHudSection("LEGAL") { legal.forEach { CenterRow(it, "Información", { onItemClick(it) }) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun CenterRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f))
        AuraStatusChip(value, if (value == "Activo" || value == "Visible" || value == "Local" || value == "Disponible") AuraCyan else AuraMuted)
    }
}
