package com.aura.defense.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.tools.LinkAnalysis
import com.aura.defense.tools.LinkRisk
import com.aura.defense.tools.PasswordAudit
import com.aura.defense.tools.PasswordStrength
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraMuted

@Composable
fun LinkAnalyzerDialog(onAnalysis: (LinkAnalysis) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf("") }
    var analysis by remember { mutableStateOf<LinkAnalysis?>(null) }
    AuraHudDialog(onDismissRequest = onDismiss, title = { Text("Analizador de enlaces") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("El análisis se realiza solo en este dispositivo. No se envía la URL.", color = AuraMuted, fontSize = 12.sp)
            OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            analysis?.let { result ->
                Text("Resultado: ${result.risk.toSpanish()}", color = when (result.risk) { LinkRisk.SEGURO -> Color(0xFF8CE6A0); LinkRisk.SOSPECHOSO -> Color(0xFFFFC66D); LinkRisk.PELIGROSO -> Color(0xFFFF8A86) }, fontSize = 17.sp)
                result.reasons.forEach { Text("• $it", color = AuraMuted, fontSize = 12.sp) }
            }
        }
    }, confirmButton = { TextButton(onClick = { val result = com.aura.defense.tools.LinkAnalyzer().analyze(value); analysis = result; onAnalysis(result) }) { Text("Analizar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}

@Composable
fun PasswordAuditorDialog(onAudit: (PasswordAudit) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf("") }
    var audit by remember { mutableStateOf<PasswordAudit?>(null) }
    DisposableEffect(Unit) { onDispose { value = "" } }
    AuraHudDialog(onDismissRequest = onDismiss, title = { Text("Auditor de contraseñas") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("La contraseña se analiza localmente, no se guarda ni se registra.", color = AuraMuted, fontSize = 12.sp)
            OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Contraseña") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            audit?.let { result ->
                Text("Resultado: ${result.strength.toSpanish()} (${result.score}/100)", color = AuraCyan, fontSize = 17.sp)
                result.recommendations.forEach { Text("• $it", color = AuraMuted, fontSize = 12.sp) }
            }
        }
    }, confirmButton = { TextButton(onClick = { val result = com.aura.defense.tools.PasswordAuditor().audit(value); audit = result; onAudit(result); value = "" }) { Text("Revisar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}

private fun LinkRisk.toSpanish() = when (this) { LinkRisk.SEGURO -> "Seguro"; LinkRisk.SOSPECHOSO -> "Sospechoso"; LinkRisk.PELIGROSO -> "Peligroso" }
private fun PasswordStrength.toSpanish() = when (this) { PasswordStrength.DEBIL -> "Débil"; PasswordStrength.MEDIA -> "Media"; PasswordStrength.FUERTE -> "Fuerte"; PasswordStrength.EXCELENTE -> "Excelente" }
