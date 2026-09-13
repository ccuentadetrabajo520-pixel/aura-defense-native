package com.aura.defense.ai.interaction

import android.content.Context
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aura.defense.ui.components.AuraButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = "${System.currentTimeMillis()}-${(0..999).random()}",
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AuraVirtualAssistant(private val context: Context) {
    private val responses = mapOf(
        "hello" to "Hola. Soy Aura, tu asistente de ciberdefensa. ¿En qué puedo ayudarte?",
        "status" to "Puedo ayudarte a revisar la seguridad, amenazas, VPN, permisos, malware, phishing, red y reportes.",
        "threats" to "Abre el análisis de amenazas para revisar los hallazgos actuales del dispositivo.",
        "scan" to "Puedes iniciar un análisis completo desde el panel principal de Aura.",
        "vpn" to "Revisa el indicador VPN del panel principal para confirmar si el tráfico está protegido.",
        "permissions" to "La auditoría de permisos muestra qué aplicaciones tienen acceso excesivo y qué puedes revisar.",
        "report" to "Puedes generar y compartir un reporte desde Herramientas o desde el panel de reportes.",
        "protection" to "Aura supervisa la postura del dispositivo, la red, las aplicaciones y los eventos de seguridad.",
        "advice" to "Mantén Android actualizado, usa una VPN en redes públicas y revisa los permisos de aplicaciones nuevas.",
        "malware" to "Ejecuta un análisis completo y revisa cualquier hallazgo de severidad media o alta.",
        "phishing" to "No abras enlaces inesperados. Usa el analizador de enlaces de Aura antes de introducir credenciales.",
        "network" to "Puedes revisar la red local y los dispositivos visibles desde el módulo de red de Aura.",
        "help" to "Pregunta por seguridad, amenazas, VPN, permisos, malware, phishing, red, consejos o reportes."
    )

    suspend fun processQuery(query: String): String {
        delay(300)
        val normalized = query.lowercase().trim()
        return when {
            normalized.isBlank() -> "Escribe una consulta para que pueda ayudarte."
            normalized.contains("hola") || normalized.contains("buenos") || normalized.contains("buenas") -> responses.getValue("hello")
            normalized.contains("segur") || normalized.contains("proteg") -> responses.getValue("status")
            normalized.contains("amenaza") || normalized.contains("peligro") -> responses.getValue("threats")
            normalized.contains("escane") || normalized.contains("analiz") -> responses.getValue("scan")
            normalized.contains("vpn") -> responses.getValue("vpn")
            normalized.contains("permiso") -> responses.getValue("permissions")
            normalized.contains("reporte") || normalized.contains("informe") -> responses.getValue("report")
            normalized.contains("proteccion") || normalized.contains("protección") || normalized.contains("defensa") -> responses.getValue("protection")
            normalized.contains("consejo") || normalized.contains("recomend") -> responses.getValue("advice")
            normalized.contains("malware") || normalized.contains("virus") -> responses.getValue("malware")
            normalized.contains("phishing") || normalized.contains("estafa") -> responses.getValue("phishing")
            normalized.contains("red") || normalized.contains("wifi") -> responses.getValue("network")
            normalized.contains("ayuda") || normalized.contains("que puedes") -> responses.getValue("help")
            else -> "No reconocí esa consulta. Prueba con seguridad, amenazas, VPN, permisos, malware, phishing, red, consejos o reportes."
        }
    }
}

@Composable
fun VirtualAssistantScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val assistant = remember { AuraVirtualAssistant(context) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text = "Hola. Soy Aura, tu asistente de ciberdefensa. ¿En qué puedo ayudarte?",
                    isUser = false
                )
            )
        )
    }
    var currentInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    fun sendMessage() {
        val query = currentInput.trim()
        if (query.isEmpty() || isProcessing) return
        messages = messages + ChatMessage(text = query, isUser = true)
        currentInput = ""
        isProcessing = true
        scope.launch {
            val response = assistant.processQuery(query)
            messages = messages + ChatMessage(text = response, isUser = false)
            isProcessing = false
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Asistente Aura", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { messages = emptyList() }) { Text("Limpiar") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages, key = { it.id }) { message -> ChatBubble(message) }
            if (isProcessing) item { Text("Aura está procesando...", style = MaterialTheme.typography.bodySmall) }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = currentInput,
                onValueChange = { currentInput = it },
                modifier = Modifier.weight(1f).onKeyEvent { event ->
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyUp) {
                        sendMessage()
                        true
                    } else false
                },
                placeholder = { Text("Pregúntale a Aura...") },
                enabled = !isProcessing,
                singleLine = true
            )
            AuraButton(
                text = if (isProcessing) "..." else "Enviar",
                onClick = ::sendMessage,
                modifier = Modifier.widthIn(min = 96.dp),
                enabled = !isProcessing && currentInput.isNotBlank()
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VirtualAssistantDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(620.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.background
        ) {
            VirtualAssistantScreen()
        }
    }
}