package com.aura.defense.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.aura.defense.tools.LinkAnalysis
import com.aura.defense.tools.LinkAnalyzer
import com.aura.defense.tools.LinkRisk

@Composable
fun ShareScannerDialog(text: String, onAnalyses: (List<LinkAnalysis>) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val analyses = remember(text) {
        extractUrls(text).map { LinkAnalyzer(context).analyze(it) }
    }
    androidx.compose.runtime.LaunchedEffect(analyses) { onAnalyses(analyses) }
    AuraHudDialog(
        onDismissRequest = onDismiss,
        title = { Text("Análisis compartido") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Texto recibido", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                Text(text.take(1000), color = Color(0xFFA9C2C0), fontSize = 12.sp)
                if (analyses.isEmpty()) {
                    Text("Sin enlaces detectados", color = Color(0xFFFFC66D), fontSize = 15.sp)
                } else {
                    Text("Enlaces detectados", color = Color(0xFF62E6D5), fontSize = 15.sp)
                    analyses.forEach { analysis ->
                        Text(analysis.url, color = Color(0xFFA9C2C0), fontSize = 12.sp)
                        Text("Resultado: ${analysis.risk.toSpanish()}", color = analysis.risk.color(), fontSize = 14.sp)
                        analysis.reasons.forEach { Text("• $it", color = Color(0xFF8BA6A4), fontSize = 12.sp) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

private fun extractUrls(text: String): List<String> = Regex("https?://[^\\s<>\\\"]+")
    .findAll(text)
    .map { it.value.trimEnd('.', ',', ';', ':', ')', ']', '}') }
    .distinct()
    .toList()

private fun LinkRisk.toSpanish() = when (this) {
    LinkRisk.SEGURO -> "Seguro"
    LinkRisk.SOSPECHOSO -> "Sospechoso"
    LinkRisk.PELIGROSO -> "Peligroso"
}

private fun LinkRisk.color() = when (this) {
    LinkRisk.SEGURO -> Color(0xFF8CE6A0)
    LinkRisk.SOSPECHOSO -> Color(0xFFFFC66D)
    LinkRisk.PELIGROSO -> Color(0xFFFF8A86)
}
