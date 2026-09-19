package com.aura.defense.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ThreatIntelligenceRepositoryProvider
import com.aura.defense.assistant.AssistantConversationService
import com.aura.defense.assistant.AssistantExplanationLevel
import com.aura.defense.assistant.AssistantProposedAction
import com.aura.defense.assistant.AssistantResponse
import com.aura.defense.assistant.AssistantResponseType
import com.aura.defense.assistant.AssistantTimelineStore
import com.aura.defense.security.PostureResult
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraSurface
import com.aura.defense.ui.AuraText
import com.aura.defense.vpn.DnsProtectionState
import com.aura.defense.vpn.DnsProtectionStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class AssistantMessage(val user: Boolean, val response: AssistantResponse)

@Composable
fun AssistantCenterScreen(
    posture: PostureResult,
    dnsState: DnsProtectionState,
    onStartDns: () -> Unit,
    onStopDns: () -> Unit,
    onScan: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val levelPreferences = remember { context.getSharedPreferences("aura_assistant", android.content.Context.MODE_PRIVATE) }
    var level by remember {
        mutableStateOf(
            runCatching { AssistantExplanationLevel.valueOf(levelPreferences.getString("explanation_level", AssistantExplanationLevel.ENTENDER.name)!!) }
                .getOrDefault(AssistantExplanationLevel.ENTENDER)
        )
    }
    var input by remember { mutableStateOf("") }
    var processing by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf(emptyList<AssistantMessage>()) }
    var showClearHistory by remember { mutableStateOf(false) }
    val service = remember(posture) {
        AssistantConversationService(
            context,
            { posture },
            actionExecutor = { action ->
                when (action.id) {
                    "start_dns" -> { onStartDns(); com.aura.defense.assistant.AssistantActionResult(action.id, true, "Solicitud de inicio enviada; el estado real aparecerá cuando Android confirme el servicio.") }
                    "stop_dns" -> { onStopDns(); com.aura.defense.assistant.AssistantActionResult(action.id, true, "Solicitud de detención enviada; el estado real se actualizará desde el servicio.") }
                    else -> com.aura.defense.assistant.AssistantActionExecutor(context) { posture }.execute(action)
                }
            }
        )
    }

    fun send() {
        val query = input.trim()
        if (query.isBlank() || processing) return
        input = ""
        processing = true
        scope.launch {
            val response = service.respond(query, level)
            messages = messages + AssistantMessage(true, AssistantResponse(AssistantResponseType.GENERAL_RESPONSE, query)) + AssistantMessage(false, response)
            processing = false
        }
    }

    val feed = ThreatIntelligenceRepositoryProvider.get(context).current().feed
    val dnsLabel = when (dnsState.status) {
        DnsProtectionStatus.ACTIVE_DNS_ONLY -> "Activa"
        DnsProtectionStatus.DEGRADED -> "Degradada"
        DnsProtectionStatus.OFF, DnsProtectionStatus.ERROR -> "Detenida"
        else -> dnsState.status.name
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF0B1016)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Cobertura actual", color = AuraText, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text("Centro local de decisiones y evidencia", color = AuraMuted, fontSize = 13.sp)
            }
            TextButton(onClick = { showClearHistory = true }) { Text("Borrar historial") }
        }
        CoverageMapCard(
            dns = dnsLabel,
            feed = ThreatIntelligenceRepositoryProvider.get(context).state().name,
            source = feed?.source ?: "No disponible",
            version = feed?.version ?: "No disponible",
            feedUpdatedAt = feed?.feedTimestamp?.let(::formatTime) ?: "No disponible",
            feedExpiresAt = feed?.expiresAt?.let(::formatTime) ?: "No disponible",
            posture = posture.status,
            postureTimestamp = posture.timestamp
        )
        val timeline = AssistantTimelineStore(context).entries().takeLast(8).reversed()
        if (timeline.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = AuraSurface), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Línea de tiempo local", color = AuraCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    timeline.forEach { event ->
                        Text("${formatTime(event.timestamp)} · ${event.kind} · ${event.source} · ${event.result}", color = AuraText, fontSize = 11.sp)
                    }
                }
            }
        } else {
            Text("Línea de tiempo local: aún no hay eventos reales persistidos.", color = AuraMuted, fontSize = 11.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistantExplanationLevel.entries.forEach { option ->
                FilterChip(selected = level == option, onClick = { level = option; levelPreferences.edit().putString("explanation_level", option.name).apply() }, label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) })
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (messages.isEmpty()) item { Text("Hola. Puedo conversar o revisar evidencia local cuando lo solicites.", color = AuraText) }
            items(messages) { message -> AssistantMessageCard(message, onConfirm = { action -> messages = messages + AssistantMessage(false, service.confirm(action)) }) }
            if (processing) item { Text("Procesando localmente...", color = AuraMuted) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Pregunta a AURA") }, enabled = !processing, singleLine = true)
            IconButton(onClick = ::send, enabled = !processing && input.isNotBlank(), modifier = Modifier.semantics { contentDescription = "Enviar pregunta" }) { Icon(Icons.Default.Send, "Enviar") }
        }
    }
    if (showClearHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistory = false },
            title = { Text("Borrar historial del asistente") },
            text = { Text("Eliminará conversaciones y acciones guardadas localmente. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { service.clearHistory(); messages = emptyList(); showClearHistory = false }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { showClearHistory = false }) { Text("Cancelar") } }
        )
    }
}

private fun formatTime(timestamp: Long): String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(timestamp))

@Composable
private fun CoverageMapCard(
    dns: String,
    feed: String,
    source: String,
    version: String,
    feedUpdatedAt: String,
    feedExpiresAt: String,
    posture: String,
    postureTimestamp: String
) {
    Card(colors = CardDefaults.cardColors(containerColor = AuraSurface), border = BorderStroke(1.dp, AuraCyan.copy(alpha = 0.2f)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Mapa de cobertura", color = AuraCyan, fontWeight = FontWeight.Bold)
            CoverageRow("Protección DNS", dns, dnsColor(dns))
            CoverageRow("Inteligencia firmada", "$feed · $version", if (feed == "CURRENT") AuraGreen else AuraAmber)
            CoverageRow("Postura disponible", posture, AuraMuted)
            Text("Feed comprobado: $feedUpdatedAt · expira: $feedExpiresAt", color = AuraMuted, fontSize = 11.sp)
            Text("Postura comprobada: $postureTimestamp", color = AuraMuted, fontSize = 11.sp)
            Text("Observa DNS local y señales visibles de Android. No inspecciona TLS, memoria privada ni garantiza ausencia de malware.", color = AuraMuted, fontSize = 11.sp)
            Text("Fuente: $source", color = AuraMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CoverageRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AuraText, fontSize = 12.sp)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.widthIn(max = 180.dp))
    }
}

private fun dnsColor(value: String): Color = if (value == "Activa") AuraGreen else AuraAmber

@Composable
private fun AssistantMessageCard(message: AssistantMessage, onConfirm: (AssistantProposedAction) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = if (message.user) Color(0xFF12343A) else AuraSurface), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (message.user) "Tú" else when (message.response.type) { AssistantResponseType.GENERAL_RESPONSE -> "Respuesta general"; AssistantResponseType.DEVICE_ASSESSMENT -> "Evaluación local"; AssistantResponseType.EVIDENCE_BASED_RECOMMENDATION -> "Recomendación basada en evidencia" }, color = AuraCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(message.response.text, color = AuraText, fontSize = 14.sp)
            message.response.citations.forEach { citation ->
                Text("Evidencia: ${citation.evidence} · ${citation.observedAt}", color = AuraMuted, fontSize = 11.sp)
                Text("Confianza: ${citation.confidence} · Límite: ${citation.limitation}", color = AuraMuted, fontSize = 11.sp)
            }
            message.response.limitations.forEach { Text("Límite: $it", color = AuraMuted, fontSize = 11.sp) }
            message.response.proposedAction?.let { action ->
                androidx.compose.material3.Button(onClick = { onConfirm(action) }) { Text("Confirmar acción") }
            }
        }
    }
}