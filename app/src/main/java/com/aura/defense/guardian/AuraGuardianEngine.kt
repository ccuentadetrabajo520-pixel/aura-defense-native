package com.aura.defense.guardian

import com.aura.defense.apps.AppRiskSeverity
import com.aura.defense.apps.AppScanResult
import com.aura.defense.notifications.NotificationAlert
import com.aura.defense.security.FindingSeverity
import com.aura.defense.security.PostureResult
import com.aura.defense.threats.ThreatIntelligenceEngine
import com.aura.defense.tools.LinkAnalysis
import com.aura.defense.tools.LinkRisk
import com.aura.defense.history.AuraHistoryEntry
import com.aura.defense.vpn.DnsBlockedEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuraGuardianEngine(private val threatEngine: ThreatIntelligenceEngine) {
    fun assess(
        posture: PostureResult,
        appScan: AppScanResult?,
        links: List<LinkAnalysis>,
        notificationAlerts: List<NotificationAlert>,
        history: List<AuraHistoryEntry> = emptyList(),
        blockedDns: List<DnsBlockedEvent> = emptyList()
    ): AuraGuardianAssessment {
        val reasons = mutableListOf<String>()
        val recommendations = mutableListOf<AuraGuardianRecommendation>()
        val missing = mutableListOf<String>()
        var score = 0

        if (posture.score >= 0) {
            if (posture.score < 60) {
                score += 2
                reasons.add("La puntuación de postura es inferior a 60")
            } else if (posture.score < 85) {
                score++
                reasons.add("La postura del dispositivo requiere atención")
            }
        } else {
            missing.add("Diagnóstico de postura pendiente")
        }
        if (posture.findings.any { it.severity >= FindingSeverity.HIGH }) {
            score++
            reasons.add("Hay hallazgos de postura de severidad alta")
        }

        if (appScan == null) {
            missing.add("Escáner de apps pendiente")
        } else if (appScan.failed) {
            missing.add("Escáner de apps no disponible")
        } else {
            if (appScan.highRiskApps.isNotEmpty()) {
                score += 2
                reasons.add("El escáner de apps encontró señales de riesgo alto")
            } else if (appScan.riskyApps.isNotEmpty()) {
                score++
                reasons.add("El escáner de apps encontró señales para revisar")
            }
            if (appScan.apps.any { app ->
                    app.grantedDangerousPermissions.isNotEmpty() &&
                        app.findings.any { it.reason == "Instalador desconocido" || it.reason == "SDK antiguo" }
                }) {
                score++
                reasons.add("Una app combina permisos sensibles con señales de instalación o versión")
            }
        }

        if (links.isEmpty()) missing.add("Sin enlaces recientes")
        val allLinks = links + notificationAlerts.map { it.analysis }
        val localMatches = allLinks.filter { it.reasons.contains("Coincidencia en inteligencia local") }
        val dangerousLinks = allLinks.count { it.risk == LinkRisk.PELIGROSO }
        if (localMatches.isNotEmpty()) {
            score += if (localMatches.any { it.risk == LinkRisk.PELIGROSO }) 3 else 2
            reasons.add("Hay coincidencias de inteligencia local en enlaces recientes")
        } else if (dangerousLinks > 0) {
            score += 2
            reasons.add("Hay enlaces peligrosos recientes para revisar")
        } else if (links.any { it.risk == LinkRisk.SOSPECHOSO }) {
            score++
            reasons.add("Hay enlaces sospechosos recientes")
        }

        if (threatEngine.indicators.isEmpty()) missing.add("Inteligencia local no disponible")
        val recentHigh = history.count { it.severity == "HIGH" }
        val recentCritical = history.count { it.severity == "CRITICAL" }
        if (history.isEmpty()) missing.add("Historial pendiente")
        if (blockedDns.isNotEmpty()) {
            score++
            reasons.add("El cortafuegos DNS bloqueó ${blockedDns.size} dominios en esta sesión")
            recommendations.add(AuraGuardianRecommendation("Revisa los dominios bloqueados por el cortafuegos DNS", 1))
        }
        if (recentHigh > 0) {
            score++
            reasons.add("Hay cambios recientes de severidad alta")
        }
        if (recentCritical > 0) {
            score += 2
            reasons.add("Hay cambios recientes de severidad crítica")
        }
        if (!posture.telemetry.vpnActive && posture.telemetry.privateDnsStatus in setOf("Inactivo", "No disponible") && dangerousLinks > 0) {
            score++
            reasons.add("VPN inactiva, DNS no activo y hay un enlace peligroso reciente")
        }
        if (posture.telemetry.vpnActive.not()) recommendations.add(AuraGuardianRecommendation("Revisa los ajustes de VPN si necesitas proteger el tráfico", 3))
        if (appScan?.highRiskApps?.isNotEmpty() == true) recommendations.add(AuraGuardianRecommendation("Revisa las apps con señales de riesgo alto", 1))
        if (dangerousLinks > 0) recommendations.add(AuraGuardianRecommendation("No abras enlaces peligrosos y revisa su origen", 1))
        if (reasons.isNotEmpty()) recommendations.add(AuraGuardianRecommendation("Revisa las señales combinadas antes de tomar decisiones", 2))

        val level = when {
            score >= 6 -> GuardianLevel.CRITICO
            score >= 4 -> GuardianLevel.RIESGO_ALTO
            score >= 2 -> GuardianLevel.ATENCION
            else -> GuardianLevel.TRANQUILO
        }
        val confidence = when {
            missing.isNotEmpty() && missing.size >= 2 -> GuardianConfidence.BAJA
            missing.isNotEmpty() || appScan == null -> GuardianConfidence.MEDIA
            else -> GuardianConfidence.ALTA
        }
        val safeReasons = reasons.distinct().take(4).ifEmpty { listOf("No se observaron señales combinadas de riesgo") }
        val safeRecommendations = recommendations.distinctBy { it.text }.sortedBy { it.priority }.map { it.text }.take(4)
        val summary = when {
            level == GuardianLevel.TRANQUILO -> "Las señales disponibles no elevan el riesgo."
            else -> "Señales combinadas elevan el riesgo. No se confirma malware, pero hay indicadores relevantes."
        }
        return AuraGuardianAssessment(
            level = level,
            confidence = confidence,
            summary = summary,
            reasons = safeReasons,
            recommendations = safeRecommendations,
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
            missingSignals = missing.distinct()
        )
    }
}
