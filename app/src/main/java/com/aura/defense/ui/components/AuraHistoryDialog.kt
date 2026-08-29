package com.aura.defense.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.history.AuraHistoryEntry
import com.aura.defense.history.AuraHistoryStore
import com.aura.defense.history.SuspiciousChangeDetector
import com.aura.defense.apps.AppScanResult
import com.aura.defense.security.PostureResult
import com.aura.defense.tools.LinkAnalysis
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraRed
import com.aura.defense.ui.AuraText

@Composable
fun AuraHistoryDialog(
    context: Context,
    posture: PostureResult,
    appScan: AppScanResult?,
    links: List<LinkAnalysis>,
    onUpdated: (List<AuraHistoryEntry>) -> Unit,
    onDismiss: () -> Unit
) {
    val store = remember { AuraHistoryStore(context) }
    var entries by remember { mutableStateOf(store.getEntries().asReversed()) }
    var message by remember { mutableStateOf<String?>(null) }
    AuraHudDialog(onDismissRequest = onDismiss, title = { Text("Historial inteligente") }, text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (entries.isEmpty()) Text("Sin eventos recientes.", color = AuraMuted)
            entries.forEach { HistoryRow(it) }
            message?.let { Text(it, color = AuraMuted, fontSize = 12.sp) }
        }
    }, confirmButton = { Button(onClick = {
        val result = SuspiciousChangeDetector(store).compare(posture, appScan, links)
        entries = store.getEntries().asReversed()
        onUpdated(entries)
        message = if (result.baselineCreated) "Línea base creada. Aura comparará cambios futuros desde este punto." else "Análisis actualizado."
    }) { Text("Actualizar análisis") } }, dismissButton = {
        TextButton(onClick = {
            if (store.clear()) { entries = emptyList(); onUpdated(emptyList()); message = "Historial borrado." }
            else message = "No se pudo borrar el historial local."
        }) { Text("Borrar historial") }
        TextButton(onClick = {
            val content = entries.joinToString("\n") { "${it.timestamp} · ${it.titleEs} · ${it.severity} · ${it.source}" }
            if (content.isBlank()) message = "Sin eventos recientes." else runCatching {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, content) }, "Exportar historial"))
                message = "Historial exportado."
            }.onFailure { message = "No se pudo exportar el historial." }
        }) { Text("Exportar historial") }
    })
}

@Composable
private fun HistoryRow(entry: AuraHistoryEntry) {
    val color = when (entry.severity) { "CRITICAL" -> AuraRed; "HIGH" -> AuraRed; "MEDIUM" -> AuraAmber; else -> AuraGreen }
    Column(modifier = Modifier.fillMaxWidth().padding(9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Cambio detectado · ${entry.titleEs}", color = color, fontSize = 14.sp)
        Text("${entry.severity.toSpanish()} · ${entry.timestamp} · ${entry.source}", color = AuraMuted, fontSize = 11.sp)
        Text(entry.descriptionEs, color = AuraText, fontSize = 12.sp)
        Text("Evidencia: ${entry.evidenceEs}", color = AuraMuted, fontSize = 11.sp)
        Text("Revisión recomendada: ${entry.recommendedActionEs}", color = AuraMuted, fontSize = 11.sp)
    }
}

private fun String.toSpanish() = when (this) {
    "LOW" -> "Bajo"
    "MEDIUM" -> "Medio"
    "HIGH" -> "Alto"
    "CRITICAL" -> "Crítico"
    else -> "No disponible"
}
