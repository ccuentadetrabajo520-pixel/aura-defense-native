package com.aura.defense.ai.interaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.aura.defense.ai.CopilotBrain
import com.aura.defense.security.PostureResult
import com.aura.defense.threats.ThreatIntelligenceEngine
import com.aura.defense.vpn.DnsFirewallProfile
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = "${System.currentTimeMillis()}-${(0..999).random()}",
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun VirtualAssistantScreen(
    modifier: Modifier = Modifier,
    posture: PostureResult,
    vpnRunning: Boolean,
    threatEngine: ThreatIntelligenceEngine?,
    onVpnToggle: () -> Unit,
    onScan: () -> Unit,
    onProfileChange: (DnsFirewallProfile) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val brain = remember(vpnRunning, posture, threatEngine) {
        CopilotBrain(context, { posture }, { vpnRunning }, threatEngine)
    }
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

    fun applyAction(action: String?) {
        when {
            action == "ACTIVATE_VPN" && !vpnRunning -> onVpnToggle()
            action == "DEACTIVATE_VPN" && vpnRunning -> onVpnToggle()
            action == "RUN_SCAN" -> onScan()
            action?.startsWith("SET_PROFILE:") == true -> runCatching {
                onProfileChange(DnsFirewallProfile.valueOf(action.substringAfter(':')))
            }
        }
    }

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
            val response = brain.process(query)
            messages = messages + ChatMessage(text = response.text, isUser = false)
            if (response.needsConfirmation == false) applyAction(response.pendingAction)
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
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("¿Qué es phishing?", "¿Qué es un troyano bancario?", "Consulta un dominio").forEach { chip ->
                AuraButton(text = chip, onClick = { currentInput = chip }, modifier = Modifier.widthIn(min = 120.dp))
            }
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
fun VirtualAssistantDialog(
    posture: PostureResult,
    vpnRunning: Boolean,
    threatEngine: ThreatIntelligenceEngine?,
    onVpnToggle: () -> Unit,
    onScan: () -> Unit,
    onProfileChange: (DnsFirewallProfile) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(620.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.background
        ) {
            VirtualAssistantScreen(
                posture = posture,
                vpnRunning = vpnRunning,
                threatEngine = threatEngine,
                onVpnToggle = onVpnToggle,
                onScan = onScan,
                onProfileChange = onProfileChange
            )
        }
    }
}