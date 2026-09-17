package com.aura.defense.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.data.AuraPreferences
import com.aura.defense.ui.components.aura.AuraCoreHologram
import com.aura.defense.ui.components.aura.AuraTermsDialog
import com.aura.defense.ui.components.aura.CoreMood
import kotlinx.coroutines.delay

@Composable
fun AuraIntroScreen(preferences: AuraPreferences, onFinished: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var termsAccepted by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    LaunchedEffect(step) {
        when (step) {
            0 -> { delay(12000); step = 1 }
            1 -> { delay(5000); step = 2 }
            2 -> { delay(8500); step = 3 }
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF05080C), Color(0xFF0D1117)))
        ).padding(24.dp)
    ) {
        when (step) {
            0 -> BootTerminal()
            1 -> OnlineScene()
            2 -> CapabilitiesScene()
            else -> ReadyScene(
                termsAccepted = termsAccepted,
                onTermsChanged = { termsAccepted = it },
                onReadTerms = { showTerms = true },
                onStart = {
                    preferences.setTermsAccepted()
                    preferences.setOnboardingCompleted()
                    onFinished()
                }
            )
        }
        if (step < 3) {
            TextButton(
                onClick = { step = 3 },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) { Text("Saltar", color = Color(0xFF657783), fontFamily = FontFamily.Monospace) }
        }
    }
    if (showTerms) AuraTermsDialog(onDismiss = { showTerms = false })
}

@Composable
private fun BootTerminal() {
    val lines = listOf(
        "AURA DEFENS v1.1",
        "> Inicializando núcleo de defensa...",
        "> Cargando inteligencia de amenazas... OK",
        "> Calibrando sensores... OK",
        "> Estableciendo vínculo seguro... OK"
    )
    var completedLines by remember { mutableStateOf(emptyList<String>()) }
    var currentLine by remember { mutableStateOf("") }
    val cursorTransition = rememberInfiniteTransition(label = "boot-cursor")
    val cursorAlpha by cursorTransition.animateFloat(
        1f, 0.3f,
        infiniteRepeatable(
            tween(400),
            RepeatMode.Reverse
        ),
        label = "boot-cursor-alpha"
    )
    LaunchedEffect(Unit) {
        lines.forEach { line ->
            currentLine = ""
            line.forEachIndexed { index, character ->
                currentLine = line.take(index + 1)
                delay(45)
            }
            completedLines = completedLines + currentLine
            currentLine = ""
            delay(900)
        }
    }
    Column(
        Modifier.fillMaxSize().padding(top = 64.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        completedLines.forEachIndexed { index, line ->
            Text(line, color = if (index == 0) Color(0xFF4DD8E6) else Color(0xFF22C55E), fontFamily = FontFamily.Monospace, fontSize = if (index == 0) 20.sp else 13.sp)
        }
        if (currentLine.isNotEmpty()) {
            Row {
                Text(currentLine, color = if (completedLines.isEmpty()) Color(0xFF4DD8E6) else Color(0xFF22C55E), fontFamily = FontFamily.Monospace, fontSize = if (completedLines.isEmpty()) 20.sp else 13.sp)
                Text("█", color = Color(0xFF22C55E).copy(alpha = cursorAlpha), fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun OnlineScene() {
    val alpha by animateFloatAsState(1f, tween(600), label = "core-boot-alpha")
    var visibleTitle by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val title = "AURA EN LÍNEA"
        title.forEachIndexed { index, _ ->
            visibleTitle = title.take(index + 1)
            delay(45)
        }
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AuraCoreHologram(CoreMood.BOOT, Modifier.size(280.dp).then(Modifier), eventPulse = 0)
        Text(visibleTitle, color = Color(0xFF4DD8E6).copy(alpha = alpha), fontFamily = FontFamily.Monospace, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(14.dp))
        Text("Hola, soy AURA. Seré tu primer asistente de ciber defensa.", color = Color.White.copy(alpha = alpha), textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    }
}

@Composable
private fun CapabilitiesScene() {
    val capabilities = listOf(
        "• Filtra 200.000+ dominios peligrosos en tiempo real",
        "• Detecta troyanos bancarios por su comportamiento",
        "• Todo se procesa en tu dispositivo. Nada sale de aquí."
    )
    var visible by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        capabilities.forEachIndexed { _, _ -> delay(800L); visible += 1 }
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AuraCoreHologram(CoreMood.IDLE, Modifier.size(250.dp))
        Spacer(Modifier.size(14.dp))
        capabilities.take(visible).forEach { line ->
            AnimatedVisibility(true, enter = fadeIn(tween(400))) {
                Text(line, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp))
            }
        }
    }
}

@Composable
private fun ReadyScene(
    termsAccepted: Boolean,
    onTermsChanged: (Boolean) -> Unit,
    onReadTerms: () -> Unit,
    onStart: () -> Unit
) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AuraCoreHologram(CoreMood.IDLE, Modifier.size(240.dp))
        Spacer(Modifier.size(10.dp))
        Text("Estoy listo para cuidarte.", color = Color(0xFF4DD8E6), fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.size(18.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onReadTerms) { Text("Leer Términos y Condiciones") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = termsAccepted, onCheckedChange = onTermsChanged)
                    Text("He leído y acepto los Términos y Condiciones", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onStart, enabled = termsAccepted, modifier = Modifier.fillMaxWidth()) { Text("Empezar") }
            }
        }
    }
}
