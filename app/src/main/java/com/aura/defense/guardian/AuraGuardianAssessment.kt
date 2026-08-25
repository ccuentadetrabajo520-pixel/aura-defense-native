package com.aura.defense.guardian

import com.aura.defense.tools.LinkAnalysis
import java.util.Locale

enum class GuardianLevel { TRANQUILO, ATENCION, RIESGO_ALTO, CRITICO }
enum class GuardianConfidence { BAJA, MEDIA, ALTA }

data class AuraGuardianAssessment(
    val level: GuardianLevel,
    val confidence: GuardianConfidence,
    val summary: String,
    val reasons: List<String>,
    val recommendations: List<String>,
    val timestamp: String,
    val missingSignals: List<String>
)

internal fun GuardianLevel.toSpanish() = when (this) {
    GuardianLevel.TRANQUILO -> "Tranquilo"
    GuardianLevel.ATENCION -> "Atención"
    GuardianLevel.RIESGO_ALTO -> "Riesgo alto"
    GuardianLevel.CRITICO -> "Crítico"
}

internal fun GuardianConfidence.toSpanish() = when (this) {
    GuardianConfidence.BAJA -> "Baja"
    GuardianConfidence.MEDIA -> "Media"
    GuardianConfidence.ALTA -> "Alta"
}

internal fun List<LinkAnalysis>.hasLocalMatch(): Boolean = any { it.reasons.contains("Coincidencia en inteligencia local") }
