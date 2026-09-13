package com.aura.defense.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ai.CopilotBrain
import com.aura.defense.ai.voice.VoiceCommandEngine
import com.aura.defense.monitor.AuraProcessLog
import com.aura.defense.security.PostureResult
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraSurface
import com.aura.defense.ui.AuraText
import com.aura.defense.ui.components.aura.AuraConsoleRobot
import com.aura.defense.ui.components.aura.AuraTerminal
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
    onScan: () -> Unit,
    onVpnToggle: () -> Unit,
    onProfileChange: (DnsFirewallProfile) -> Unit,
    onModuleDialog: (String, String) -> Unit
) {
    val context = LocalContext.current
    val entries by AuraProcessLog.entries.collectAsState()
    val voiceEngine = remember { VoiceCommandEngine(context) }
    val voiceResult by voiceEngine.commandResult.collectAsState()
    val voiceError by voiceEngine.error.collectAsState()
    val messages = remember { mutableStateListOf<Pair<Boolean, String>>() }
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    val brain = remember(posture, vpnRunning) {
        CopilotBrain(context, { posture }, { vpnRunning }, null)
    }

    fun addMessage(isUser: Boolean, text: String) {
        if (text.isBlank()) return
        while (messages.size >= 12) messages.removeAt(0)
        messages.add(isUser to text)
    }

    fun normalizeVoiceCommand(command: String): String = when (command) {
        "scan" -> "escanea mis apps"
        "vpn_on" -> "activa la vpn"
        "vpn_off" -> "desactiva la vpn"
        "vpn_status" -> "como esta mi vpn"
        "security_status", "protection_status" -> "como esta mi seguridad"
        "reports" -> "genera un reporte"
        "threats" -> "que amenazas hay"
        else -> command
    }

    fun processInput(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank()) return
        addMessage(true, text)
        scope.launch(Dispatchers.IO) {
            val response = brain.process(text)
            withContext(Dispatchers.Main) {
                if (response.needsConfirmation != true) {
                    when (response.pendingAction) {
                        "ACTIVATE_VPN", "DEACTIVATE_VPN" -> onVpnToggle()
                        "RUN_SCAN" -> onScan()
                        else -> Unit
                    }
                    response.pendingAction?.takeIf { it.startsWith("SET_PROFILE:") }
                        ?.substringAfter(':')
                        ?.let { profileName ->
                            DnsFirewallProfile.entries.firstOrNull { it.name == profileName }
                                ?.let(onProfileChange)
                        }
                }
                addMessage(false, response.text)
            }
        }
    }

    LaunchedEffect(Unit) {
        addMessage(false, greetingForHour())
    }
    LaunchedEffect(voiceResult) {
        voiceResult?.let {
            processInput(normalizeVoiceCommand(it))
            voiceEngine.clearResult()
        }
    }
    LaunchedEffect(voiceError) {
        voiceError?.let {
            addMessage(false, "No pude escucharte claramente")
            voiceEngine.clearResult()
        }
    }
    DisposableEffect(voiceEngine) {
        onDispose { voiceEngine.destroy() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AuraTerminal(
                entries = entries,
                modifier = Modifier.weight(0.62f).fillMaxSize()
            )
            AuraConsoleRobot(
                modifier = Modifier.weight(0.38f),
                serious = guardianSerious,
                scanning = scanning,
                eventCount = entries.size
            )
        }
        Text(
            "¿En qué puedo ayudarte hoy?",
            color = Color(0xFF4DD8E6),
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("¿Por qué mi score?", "Escanea mis apps", "¿Cómo está mi red?", "¿Qué es phishing?").forEach { chip ->
                Text(
                    chip,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { processInput(chip) }
                        .background(AuraSurface, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    color = AuraMuted,
                    fontSize = 10.sp,
                    maxLines = 2
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages) { (isUser, text) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        text,
                        modifier = Modifier
                            .background(
                                if (isUser) Color(0xFF064E5A) else Color(0xFF252A31),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(9.dp),
                        color = if (isUser) Color(0xFFB8F3F7) else AuraText,
                        fontSize = 12.sp
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe o habla…") },
                singleLine = true
            )
            IconButton(onClick = { voiceEngine.startListening() }) {
                Icon(Icons.Default.Mic, contentDescription = "Hablar")
            }
            IconButton(onClick = { processInput(input); input = "" }) {
                Icon(Icons.Default.Send, contentDescription = "Enviar")
            }
        }
    }
}

private fun greetingForHour(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Buenos días."
        in 12..18 -> "Buenas tardes."
        else -> "Buenas noches."
    }
    return "$greeting ¿En qué puedo ayudarte hoy?"
}
