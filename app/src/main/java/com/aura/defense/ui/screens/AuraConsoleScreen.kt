package com.aura.defense.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.defense.ai.AuraVoice
import com.aura.defense.ai.CopilotBrain
import com.aura.defense.ai.voice.VoiceCommandEngine
import com.aura.defense.monitor.AuraProcessLog
import com.aura.defense.security.PostureResult
import com.aura.defense.ui.AuraText
import com.aura.defense.ui.components.aura.AuraBot
import com.aura.defense.ui.components.aura.AuraTerminal
import com.aura.defense.ui.components.aura.BotMood
import com.aura.defense.vpn.DnsFirewallProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuraConsoleScreen(
    posture: PostureResult,
    guardianSerious: Boolean,
    scanning: Boolean,
    vpnRunning: Boolean,
    inBackground: Boolean = false,
    onScan: () -> Unit,
    onVpnToggle: () -> Unit,
    onProfileChange: (DnsFirewallProfile) -> Unit,
    onModuleDialog: (String, String) -> Unit
) {
    val context = LocalContext.current
    val entries by AuraProcessLog.entries.collectAsState()
    val voiceEngine = remember { VoiceCommandEngine(context) }
    val voice = remember { AuraVoice(context) }
    val voiceResult by voiceEngine.commandResult.collectAsState()
    val voiceProcessing by voiceEngine.isProcessing.collectAsState()
    val voiceError by voiceEngine.error.collectAsState()
    val messages = remember { mutableStateListOf<Pair<Boolean, String>>() }
    val scope = rememberCoroutineScope()
    val lifecycleState by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val resumed = lifecycleState == Lifecycle.State.RESUMED
    var input by remember { mutableStateOf("") }
    var voiceEnabled by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    var proud by remember { mutableStateOf(false) }
    var processingQuestion by remember { mutableStateOf(false) }
    var handledEvent by remember { mutableStateOf<Long?>(null) }
    val redEvents = entries.count { it.category == "RED" }
    val lastSecurityEvent = entries.lastOrNull { it.category == "RED" || it.category == "SCAN" && it.message.startsWith("Escaneo completado") }
    val brain = remember(posture, vpnRunning) { CopilotBrain(context, { posture }, { vpnRunning }, null) }

    LaunchedEffect(lastSecurityEvent?.timestamp, resumed) {
        if (lastSecurityEvent != null && lastSecurityEvent.timestamp != handledEvent) {
            handledEvent = lastSecurityEvent.timestamp
            proud = true
            kotlinx.coroutines.delay(2000)
            proud = false
        }
    }
    LaunchedEffect(Unit) { voice.onSpeakingChanged = { speaking = it } }
    LaunchedEffect(Unit) { messages.add(false to greetingForHour()) }
    LaunchedEffect(voiceResult) {
        voiceResult?.let {
            processInput(it, onScan, onVpnToggle, onProfileChange, brain, messages, scope, { processingQuestion = it }) { response ->
                if (voiceEnabled) voice.speak(response)
            }
            voiceEngine.clearResult()
        }
    }
    LaunchedEffect(voiceError) {
        voiceError?.let { messages.add(false to "No pude escucharte claramente"); voiceEngine.clearResult() }
    }
    DisposableEffect(voiceEngine) { onDispose { voiceEngine.destroy(); voice.destroy() } }

    val mood = when {
        speaking -> BotMood.HABLANDO
        scanning -> BotMood.SCANNING
        guardianSerious -> BotMood.SERIO
        processingQuestion || voiceProcessing -> BotMood.THINKING
        proud -> BotMood.ORGULLOSO
        else -> BotMood.IDLE
    }
    Column(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D1117), Color(0xFF10141B))))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().weight(0.42f), contentAlignment = Alignment.Center) {
            AuraBot(mood = mood, modifier = Modifier.fillMaxSize(), isSpeaking = speaking, pointAtTerminalEvent = redEvents)
        }
        Box(Modifier.fillMaxWidth().weight(0.18f)) { AuraTerminal(entries, Modifier.fillMaxSize()) }
        Column(Modifier.fillMaxWidth().weight(0.40f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                itemsIndexed(messages.takeLast(3)) { _, (isUser, text) ->
                    AnimatedVisibility(true, enter = fadeIn() + slideInHorizontally(initialOffsetX = { if (isUser) 30 else -30 })) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                            Text(text, color = if (isUser) Color(0xFFB8F3F7) else AuraText, fontSize = 12.sp,
                                modifier = Modifier.background(if (isUser) Color(0xDD064E5A) else Color(0xDD252A31), RoundedCornerShape(10.dp)).padding(9.dp))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Escribe o habla…") }, singleLine = true)
                IconButton(onClick = { voiceEngine.startListening() }) { Icon(Icons.Default.Mic, "Hablar") }
                IconButton(onClick = { voiceEnabled = !voiceEnabled; if (!voiceEnabled) voice.stop() else messages.add(false to "Voz activada. Te responderé hablando.") }) {
                    Icon(if (voiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff, if (voiceEnabled) "Desactivar voz" else "Activar voz")
                }
                IconButton(onClick = {
                    processInput(input, onScan, onVpnToggle, onProfileChange, brain, messages, scope, { processingQuestion = it }) { response ->
                        if (voiceEnabled) voice.speak(response)
                    }
                    input = ""
                }) { Icon(Icons.Default.Send, "Enviar") }
            }
        }
    }

    LaunchedEffect(speaking, voiceEnabled) { if (speaking && !voiceEnabled) voice.stop() }
}

private fun processInput(
    rawText: String,
    onScan: () -> Unit,
    onVpnToggle: () -> Unit,
    onProfileChange: (DnsFirewallProfile) -> Unit,
    brain: CopilotBrain,
    messages: MutableList<Pair<Boolean, String>>,
    scope: kotlinx.coroutines.CoroutineScope,
    setProcessing: (Boolean) -> Unit,
    onResponse: (String) -> Unit
) {
    val text = rawText.trim()
    if (text.isBlank()) return
    while (messages.size >= 12) messages.removeAt(0)
    messages.add(true to text)
    setProcessing(true)
    scope.launch(Dispatchers.IO) {
        val response = runCatching { brain.process(text) }.getOrElse {
            com.aura.defense.ai.CopilotResponse("Tuve un problema procesando eso. Inténtalo de nuevo.", false)
        }
        withContext(Dispatchers.Main) {
            if (response.needsConfirmation != true) {
                when (response.pendingAction) {
                    "ACTIVATE_VPN", "DEACTIVATE_VPN" -> onVpnToggle()
                    "RUN_SCAN" -> onScan()
                }
                response.pendingAction?.substringAfter("SET_PROFILE:", "").takeIf { it.isNotBlank() }?.let { name ->
                    DnsFirewallProfile.entries.firstOrNull { it.name == name }?.let(onProfileChange)
                }
            }
            while (messages.size >= 12) messages.removeAt(0)
            messages.add(false to response.text)
            onResponse(response.text)
            setProcessing(false)
        }
    }
}

private fun greetingForHour(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return "${if (hour in 5..11) "Buenos días" else if (hour in 12..18) "Buenas tardes" else "Buenas noches"}. ¿En qué puedo ayudarte hoy?"
}
