package com.aura.defense.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.aura.defense.guardian.AuraGuardianAssessment
import com.aura.defense.guardian.GuardianLevel
import com.aura.defense.guardian.toSpanish
import com.aura.defense.ui.AuraAmber
import com.aura.defense.ui.AuraCyan
import com.aura.defense.ui.AuraGreen
import com.aura.defense.ui.AuraMuted
import com.aura.defense.ui.AuraRed
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun AuraGuardianDialog(assessment: AuraGuardianAssessment, onDismiss: () -> Unit) {
    val color = assessment.level.color()
    AuraHudDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Guardián Aura")
                AuraStatusChip(assessment.level.toSpanish(), color)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(assessment.summary, color = AuraMuted, fontSize = 13.sp)
                Text("Confianza: ${assessment.confidence.toSpanish()}", color = AuraCyan, fontSize = 13.sp)
                Text("Razones principales", color = AuraCyan, fontSize = 12.sp)
                assessment.reasons.forEach { Text("• $it", color = Color(0xFFEAF7F5), fontSize = 12.sp) }
                Text("Acciones recomendadas", color = AuraCyan, fontSize = 12.sp)
                assessment.recommendations.forEach { Text("• $it", color = Color(0xFFEAF7F5), fontSize = 12.sp) }
                if (assessment.missingSignals.isNotEmpty()) {
                    Text("Señales pendientes", color = AuraAmber, fontSize = 12.sp)
                    assessment.missingSignals.forEach { Text("• $it", color = AuraMuted, fontSize = 12.sp) }
                }
                Text("Marca temporal: ${assessment.timestamp}", color = AuraMuted, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
fun AuraGuardianPanel(assessment: AuraGuardianAssessment, onViewAnalysis: () -> Unit) {
    val color = assessment.level.color()
    Surface(
        color = color.copy(alpha = 0.09f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Guardián Aura", color = AuraCyan, fontSize = 12.sp)
            AuraStatusChip(assessment.level.toSpanish(), color)
            Text(assessment.summary, color = AuraMuted, fontSize = 12.sp, maxLines = 2)
            TextButton(onClick = onViewAnalysis) { Text("Ver análisis") }
        }
    }
}

private fun GuardianLevel.color() = when (this) {
    GuardianLevel.TRANQUILO -> AuraGreen
    GuardianLevel.ATENCION -> AuraAmber
    GuardianLevel.RIESGO_ALTO -> AuraRed
    GuardianLevel.CRITICO -> AuraRed
}
