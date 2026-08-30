package com.aura.defense.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraSpacing

@Composable
fun AuraToolsHubDialog(
        onDismiss: () -> Unit
) {
        AuraHudDialog(
                    onDismissRequest = onDismiss,
                            title = { Text("Herramientas Avanzadas") },
                                    text = {
                                                    Column(
                                                                        modifier = Modifier.verticalScroll(rememberScrollState()),
                                                                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                                        ToolEntry("Integridad del dispositivo", "9 controles: root, emulador, debug, desarrollador, fuentes desconocidas, Play Services, bloqueo seguro, estado de bloqueo, cifrado.")
                                                                                        ToolEntry("Auditoría de permisos", "Escanea todas las apps no-sistema para detectar permisos peligrosos y combinaciones sospechosas.")
                                                                                                        ToolEntry("Verificación de contraseñas (HIBP)", "Verifica si una contraseña ha aparecido en filtraciones usando la API k-anonymity de HaveIBeenPwned. No envía la contraseña completa.")
                                                                                                                        ToolEntry("Certificado de seguridad", "Genera un certificado PDF con la puntuación, integridad y clasificación del dispositivo. Compartible.")
                                                                                                                                        ToolEntry("Historial de puntuaciones", "Muestra las últimas 50 puntuaciones con tendencia y promedio.")
                                                                                                                                                        ToolEntry("Generar reporte PDF", "Genera un reporte visual en PDF con los hallazgos de seguridad actuales.")
                                                                                                                                                                        Text("Todas las herramientas se ejecutan localmente en el dispositivo. Aura no envía datos a servidores externos excepto la verificación HIBP (k-anonymity, la contraseña nunca sale del dispositivo).", color = AuraMuted, fontSize = 11.sp)
                                                    }
                                    },
                                            confirmButton = {
                                                            TextButton(onClick = onDismiss) { Text("Cerrar") }
                                            }
        )
}

@Composable
private fun ToolEntry(name: String, description: String) {
        Column(modifier = Modifier.padding(vertical = AuraSpacing.xs)) {
                    Text(name, color = AuraCyan, fontSize = 13.sp)
                            Text(description, color = AuraMuted, fontSize = 11.sp)
        }
}
