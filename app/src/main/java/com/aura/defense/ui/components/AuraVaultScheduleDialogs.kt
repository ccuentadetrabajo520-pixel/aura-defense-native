package com.aura.defense.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.scheduler.AuraSchedule
import com.aura.defense.scheduler.scheduleAuraChecks
import com.aura.defense.vault.AuraVault
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted

@Composable
fun AuraVaultDialog(context: Context, onDismiss: () -> Unit) {
    val vault = remember { AuraVault(context) }
    val available = remember { vault.isAvailable() }
    var message by remember { mutableStateOf<String?>(null) }
    AuraHudDialog(onDismissRequest = onDismiss, title = { Text("Bóveda cifrada") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (available) "Bóveda disponible en este dispositivo." else "Bóveda no disponible en este dispositivo.", color = if (available) AuraGreen else AuraAmber)
            if (available) vault.readSummary()?.let { Text("Último resumen guardado: $it", color = AuraMuted, fontSize = 12.sp) }
                ?: Text("No hay historial guardado.", color = AuraMuted, fontSize = 12.sp)
            message?.let { Text(it, color = AuraMuted, fontSize = 12.sp) }
            TextButton(onClick = {
                val summary = vault.readSummary()
                if (summary == null) {
                    message = "No hay historial guardado."
                } else {
                    runCatching {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, summary)
                        }, "Exportar historial"))
                    }.onSuccess { message = "Historial exportado." }.onFailure { message = "No se pudo acceder a la bóveda cifrada." })
                }
            }) { Text("Exportar historial") }
        }
    }, confirmButton = { Button(onClick = { message = if (vault.saveSummary("Resumen guardado localmente")) "Reporte guardado en la bóveda." else "No se pudo acceder a la bóveda cifrada." }) { Text("Guardar reporte en bóveda") } }, dismissButton = { TextButton(onClick = { message = if (vault.clear()) "Historial borrado." else "No se pudo acceder a la bóveda cifrada." }) { Text("Borrar historial") } })
}

@Composable
fun AuraScheduleDialog(context: Context, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(AuraSchedule.APAGADO) }
    var message by remember { mutableStateOf<String?>(null) }
    AuraHudDialog(onDismissRequest = onDismiss, title = { Text("Comprobaciones programadas") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Selecciona una programación para comprobaciones locales.", color = AuraMuted, fontSize = 12.sp)
            AuraSchedule.values().forEach { schedule -> TextButton(onClick = { selected = schedule }) { Text(if (schedule == selected) "✓ ${schedule.toSpanish()}" else schedule.toSpanish()) } }
            message?.let { Text(it, color = AuraGreen, fontSize = 12.sp) }
        }
    }, confirmButton = { Button(onClick = { message = if (scheduleAuraChecks(context, selected)) "Programación guardada: ${selected.toSpanish()}." else "No se pudo guardar la programación." }) { Text("Guardar programación") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}

private fun AuraSchedule.toSpanish() = when (this) { AuraSchedule.APAGADO -> "Apagado"; AuraSchedule.DIARIO -> "Diario"; AuraSchedule.SEMANAL -> "Semanal" }
