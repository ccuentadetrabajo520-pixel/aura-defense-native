package com.aura.defense.reports

import com.aura.defense.apps.AppScanResult
import com.aura.defense.data.DeviceTelemetrySnapshot
import com.aura.defense.security.PostureResult
import com.aura.defense.tools.LinkAnalysis
import com.aura.defense.tools.PasswordAudit
import com.aura.defense.notifications.NotificationAlert
import com.aura.defense.threats.ThreatIndicator
import com.aura.defense.threats.ThreatIntelligenceSnapshot
import com.aura.defense.guardian.AuraGuardianAssessment
import com.aura.defense.guardian.GuardianConfidence
import com.aura.defense.guardian.GuardianLevel
import com.aura.defense.files.AuraFileAnalysis
import com.aura.defense.lan.AuraLanPeer
import com.aura.defense.history.AuraHistoryEntry
import java.util.Locale

class AuraReportBuilder {
    fun text(auraId: String, posture: PostureResult, apps: AppScanResult?, links: List<LinkAnalysis>, password: PasswordAudit?, notifications: List<NotificationAlert> = emptyList(), indicators: List<ThreatIndicator> = emptyList(), guardian: AuraGuardianAssessment? = null, file: AuraFileAnalysis? = null, vaultAvailable: Boolean = false, lanPeers: List<AuraLanPeer> = emptyList(), lastLanScan: String? = null, history: List<AuraHistoryEntry> = emptyList(), baselineTimestamp: String = "No disponible", threatSnapshot: ThreatIntelligenceSnapshot? = null): String = buildString {
        appendLine("INFORME AURA DEFENS")
        appendLine("Fecha: ${posture.timestamp}")
        appendLine("Aura ID: $auraId")
        appendLine("Puntuación Aura: ${if (posture.score >= 0) posture.score else "No disponible"}")
        appendLine("Estado: ${posture.status}")
        appendLine()
        appendLine("TELEMETRÍA")
        appendTelemetry(posture.telemetry)
        appendLine()
        appendLine("ESCÁNER DE APPS")
        appendLine(apps?.let { "Apps visibles: ${it.apps.size}\nApps con riesgos: ${it.riskyApps.size}\nRiesgo alto: ${it.highRiskApps.size}" } ?: "No se ha ejecutado un escaneo de apps")
        if (apps != null) apps.riskyApps.forEach { app -> appendLine("- ${app.appName} (${app.packageName}): ${app.findings.joinToString(", ") { it.reason }}") }
        appendLine()
        appendLine("ANÁLISIS DE ENLACES")
        if (links.isEmpty()) appendLine("No se han analizado enlaces") else links.forEach { appendLine("- ${it.risk}: ${it.url} | ${it.reasons.joinToString(", ")}") }
        appendLine()
        appendLine("AUDITORÍA DE CONTRASEÑA")
        appendLine(password?.let { "Resultado: ${it.strength.name.lowercase(Locale.getDefault())}, puntuación ${it.score}/100. La contraseña no se incluye." } ?: "No se ha ejecutado una auditoría")
        appendLine()
        appendLine("PROTECCIÓN DE NOTIFICACIONES")
        appendNotificationSummary(notifications)
        appendLine()
        appendLine("INTELIGENCIA LOCAL DE AMENAZAS")
        appendThreatMetadata(threatSnapshot, indicators)
        appendLine("Indicadores cargados: ${indicators.size}")
        appendLine("Última actualización incluida: ${indicators.maxOfOrNull { it.updatedAt } ?: "No disponible"}")
        appendLine("Coincidencias recientes: ${countThreatMatches(links, notifications)}")
        appendGuardian(guardian)
        appendFileSummary(file, vaultAvailable)
        appendLine()
        appendLine("AURAS LAN")
        appendLine("Última comprobación: ${lastLanScan ?: "No disponible"}")
        appendLine("Auras encontradas: ${lanPeers.size}")
        appendHistorySummary(history, baselineTimestamp)
        appendLine()
        appendLine("Límites reales de Android sin root: las señales disponibles dependen de la versión, permisos y fabricante. Este informe no confirma malware.")
    }

    fun json(auraId: String, posture: PostureResult, apps: AppScanResult?, links: List<LinkAnalysis>, password: PasswordAudit?, notifications: List<NotificationAlert> = emptyList(), indicators: List<ThreatIndicator> = emptyList(), guardian: AuraGuardianAssessment? = null, file: AuraFileAnalysis? = null, vaultAvailable: Boolean = false, lanPeers: List<AuraLanPeer> = emptyList(), lastLanScan: String? = null, history: List<AuraHistoryEntry> = emptyList(), baselineTimestamp: String = "No disponible", threatSnapshot: ThreatIntelligenceSnapshot? = null): String {
        fun quote(value: String) = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        return "{" + listOf(
            "\"fecha\":${quote(posture.timestamp)}",
            "\"auraId\":${quote(auraId)}",
            "\"puntuacion\":${posture.score}",
            "\"estado\":${quote(posture.status)}",
            "\"appsVisibles\":${apps?.apps?.size ?: 0}",
            "\"appsConRiesgos\":${apps?.riskyApps?.size ?: 0}",
            "\"riesgoAlto\":${apps?.highRiskApps?.size ?: 0}",
            "\"enlacesAnalizados\":${links.size}",
            "\"enlacesNotificacionesAnalizados\":${notifications.size}",
            "\"enlacesNotificacionesSospechosos\":${notifications.count { it.analysis.risk.name == "SOSPECHOSO" }}",
            "\"enlacesNotificacionesPeligrosos\":${notifications.count { it.analysis.risk.name == "PELIGROSO" }}",
            "\"indicadoresInteligenciaLocal\":${indicators.size}",
            "\"versionInteligenciaAmenazas\":${quote(threatSnapshot?.version ?: "local-compatible")}",
            "\"fuenteInteligenciaAmenazas\":${quote(threatSnapshot?.source ?: "Inteligencia local incluida")}",
            "\"baseInteligenciaActualizada\":${threatSnapshot?.isUpdated ?: false}",
            "\"estadoActualizacionInteligencia\":${quote(threatSnapshot?.lastUpdateStatus ?: "No disponible")}",
            "\"actualizacionInteligenciaLocal\":${quote(indicators.maxOfOrNull { it.updatedAt } ?: "No disponible")}",
            "\"coincidenciasInteligenciaLocal\":${countThreatMatches(links, notifications)}",
            "\"guardianNivel\":${quote(guardian?.level?.toSpanish() ?: "No disponible")}",
            "\"guardianConfianza\":${quote(guardian?.confidence?.toSpanish() ?: "No disponible")}",
            "\"guardianRazones\":${quote(guardian?.reasons?.joinToString("; ") ?: "No disponible")}",
            "\"guardianRecomendaciones\":${quote(guardian?.recommendations?.joinToString("; ") ?: "No disponible")}",
            "\"guardianFecha\":${quote(guardian?.timestamp ?: "No disponible")}",
            "\"archivoAnalizado\":${quote(file?.name ?: "No disponible")}",
            "\"archivoRiesgo\":${quote(file?.risk ?: "No disponible")}",
            "\"bovedaDisponible\":$vaultAvailable",
            "\"ultimaComprobacionLan\":${quote(lastLanScan ?: "No disponible")}",
            "\"aurasLanEncontradas\":${lanPeers.size}",
            "\"historialEventos\":${history.size}",
            "\"historialBajos\":${history.count { it.severity == "LOW" }}",
            "\"historialMedios\":${history.count { it.severity == "MEDIUM" }}",
            "\"historialAltos\":${history.count { it.severity == "HIGH" }}",
            "\"historialCriticos\":${history.count { it.severity == "CRITICAL" }}",
            "\"lineaBaseFecha\":${quote(baselineTimestamp)}",
            "\"auditoriaContrasena\":${password?.let { quote(it.strength.name) } ?: "null"}",
            "\"limitesAndroid\":${quote("Las señales dependen de la versión, permisos y fabricante; no confirma malware")}" 
        ).joinToString(",") + "}"
    }

    private fun StringBuilder.appendNotificationSummary(alerts: List<NotificationAlert>) {
        appendLine("Enlaces analizados: ${alerts.size}")
        appendLine("Sospechosos: ${alerts.count { it.analysis.risk.name == "SOSPECHOSO" }}")
        appendLine("Peligrosos: ${alerts.count { it.analysis.risk.name == "PELIGROSO" }}")
    }

    private fun StringBuilder.appendThreatMetadata(snapshot: ThreatIntelligenceSnapshot?, indicators: List<ThreatIndicator>) {
        appendLine("Versión: ${snapshot?.version ?: "local-compatible"}")
        appendLine("Última actualización: ${snapshot?.updatedAt ?: indicators.maxOfOrNull { it.updatedAt } ?: "No disponible"}")
        appendLine("Fuente: ${snapshot?.source ?: "Inteligencia local incluida"}")
        appendLine("Base activa: ${if (snapshot?.isUpdated == true) "Base actualizada" else "Base incluida"}")
        appendLine("Estado de actualización: ${snapshot?.lastUpdateStatus ?: "No disponible"}")
    }

    private fun countThreatMatches(links: List<LinkAnalysis>, notifications: List<NotificationAlert>): Int =
        links.count { it.reasons.contains("Coincidencia en inteligencia local") } +
            notifications.count { it.analysis.reasons.contains("Coincidencia en inteligencia local") }

    private fun StringBuilder.appendGuardian(guardian: AuraGuardianAssessment?) {
        appendLine()
        appendLine("GUARDIÁN AURA")
        appendLine("Nivel: ${guardian?.level?.toSpanish() ?: "No disponible"}")
        appendLine("Confianza: ${guardian?.confidence?.toSpanish() ?: "No disponible"}")
        appendLine("Razones: ${guardian?.reasons?.joinToString("; ") ?: "No disponible"}")
        appendLine("Recomendaciones: ${guardian?.recommendations?.joinToString("; ") ?: "No disponible"}")
        appendLine("Fecha: ${guardian?.timestamp ?: "No disponible"}")
    }

    private fun StringBuilder.appendFileSummary(file: AuraFileAnalysis?, vaultAvailable: Boolean) {
        appendLine()
        appendLine("ANÁLISIS DE ARCHIVOS")
        appendLine("Archivo: ${file?.name ?: "No disponible"}")
        appendLine("Riesgo potencial: ${file?.risk ?: "No disponible"}")
        appendLine("Bóveda cifrada: ${if (vaultAvailable) "Disponible" else "No disponible"}")
    }

    private fun StringBuilder.appendHistorySummary(history: List<AuraHistoryEntry>, baselineTimestamp: String) {
        appendLine()
        appendLine("HISTORIAL INTELIGENTE")
        appendLine("Eventos recientes: ${history.size}")
        appendLine("Bajos: ${history.count { it.severity == "LOW" }}")
        appendLine("Medios: ${history.count { it.severity == "MEDIUM" }}")
        appendLine("Altos: ${history.count { it.severity == "HIGH" }}")
        appendLine("Críticos: ${history.count { it.severity == "CRITICAL" }}")
        appendLine("Última línea base: $baselineTimestamp")
    }

    private fun GuardianLevel.toSpanish() = when (this) {
        GuardianLevel.TRANQUILO -> "Tranquilo"
        GuardianLevel.ATENCION -> "Atención"
        GuardianLevel.RIESGO_ALTO -> "Riesgo alto"
        GuardianLevel.CRITICO -> "Crítico"
    }

    private fun GuardianConfidence.toSpanish() = when (this) {
        GuardianConfidence.BAJA -> "Baja"
        GuardianConfidence.MEDIA -> "Media"
        GuardianConfidence.ALTA -> "Alta"
    }

    fun markdown(auraId: String, posture: PostureResult, apps: AppScanResult?, links: List<LinkAnalysis>, password: PasswordAudit?, notifications: List<NotificationAlert> = emptyList(), indicators: List<ThreatIndicator> = emptyList(), guardian: AuraGuardianAssessment? = null, file: AuraFileAnalysis? = null, vaultAvailable: Boolean = false, lanPeers: List<AuraLanPeer> = emptyList(), lastLanScan: String? = null, history: List<AuraHistoryEntry> = emptyList(), baselineTimestamp: String = "No disponible", threatSnapshot: ThreatIntelligenceSnapshot? = null): String = buildString {
        appendLine("# Informe Aura Defense")
        appendLine()
        appendLine("- Fecha: ${posture.timestamp}")
        appendLine("- Aura ID: $auraId")
        appendLine("- Puntuación Aura: ${if (posture.score >= 0) posture.score else "No disponible"}")
        appendLine("- Estado: ${posture.status}")
        appendLine()
        appendLine("## Telemetría")
        appendLine("- Fabricante: ${posture.telemetry.manufacturer}")
        appendLine("- Modelo: ${posture.telemetry.model}")
        appendLine("- Android: ${posture.telemetry.androidVersion} (API ${posture.telemetry.apiLevel})")
        appendLine("- Parche de seguridad: ${posture.telemetry.securityPatch}")
        appendLine("- Batería: ${posture.telemetry.batteryLevel}")
        appendLine("- Red: ${posture.telemetry.networkActive}")
        appendLine("- VPN: ${if (posture.telemetry.vpnActive) "Activa" else "Inactiva"}")
        appendLine("- DNS privado: ${posture.telemetry.privateDnsStatus}")
        appendLine()
        appendLine("## Escáner de apps")
        appendLine("Apps visibles: ${apps?.apps?.size ?: 0}")
        appendLine("Apps con riesgos: ${apps?.riskyApps?.size ?: 0}")
        appendLine("Riesgo alto: ${apps?.highRiskApps?.size ?: 0}")
        if (apps != null) {
            apps.riskyApps.forEach { app ->
                appendLine("- ${app.appName} (${app.packageName}): ${app.findings.joinToString(", ") { it.reason }}")
            }
        }
        appendLine()
        appendLine("## Análisis de enlaces")
        if (links.isEmpty()) appendLine("No se han analizado enlaces") else links.forEach { appendLine("- ${it.risk}: ${it.url} | ${it.reasons.joinToString(", ")}") }
        appendLine()
        appendLine("## Auditoría de contraseña")
        appendLine(password?.let { "Resultado: ${it.strength.name.lowercase(Locale.getDefault())}, puntuación ${it.score}/100." } ?: "No se ha ejecutado una auditoría")
        appendLine()
        appendLine("## Protección de notificaciones")
        appendLine("- Enlaces analizados: ${notifications.size}")
        appendLine("- Sospechosos: ${notifications.count { it.analysis.risk.name == "SOSPECHOSO" }}")
        appendLine("- Peligrosos: ${notifications.count { it.analysis.risk.name == "PELIGROSO" }}")
        appendLine()
        appendLine("## Inteligencia local de amenazas")
        appendLine("- Versión: ${threatSnapshot?.version ?: "local-compatible"}")
        appendLine("- Última actualización: ${threatSnapshot?.updatedAt ?: indicators.maxOfOrNull { it.updatedAt } ?: "No disponible"}")
        appendLine("- Fuente: ${threatSnapshot?.source ?: "Inteligencia local incluida"}")
        appendLine("- Coincidencias recientes: ${countThreatMatches(links, notifications)}")
        appendLine()
        appendLine("## Guardián Aura")
        appendLine("- Nivel: ${guardian?.level?.toSpanish() ?: "No disponible"}")
        appendLine("- Confianza: ${guardian?.confidence?.toSpanish() ?: "No disponible"}")
        appendLine("- Razones: ${guardian?.reasons?.joinToString("; ") ?: "No disponible"}")
        appendLine("- Recomendaciones: ${guardian?.recommendations?.joinToString("; ") ?: "No disponible"}")
        appendLine()
        appendLine("## Análisis de archivos")
        appendLine("- Archivo: ${file?.name ?: "No disponible"}")
        appendLine("- Riesgo potencial: ${file?.risk ?: "No disponible"}")
        appendLine("- Bóveda cifrada: ${if (vaultAvailable) "Disponible" else "No disponible"}")
        appendLine()
        appendLine("## Historial inteligente")
        appendLine("- Eventos recientes: ${history.size}")
        appendLine("- Bajos: ${history.count { it.severity == "LOW" }}")
        appendLine("- Medios: ${history.count { it.severity == "MEDIUM" }}")
        appendLine("- Altos: ${history.count { it.severity == "HIGH" }}")
        appendLine("- Críticos: ${history.count { it.severity == "CRITICAL" }}")
        appendLine("- Última línea base: $baselineTimestamp")
        appendLine()
        appendLine("_Límites reales de Android sin root: las señales disponibles dependen de la versión, permisos y fabricante. Este informe no confirma malware._")
    }

    fun html(auraId: String, posture: PostureResult, apps: AppScanResult?, links: List<LinkAnalysis>, password: PasswordAudit?, notifications: List<NotificationAlert> = emptyList(), indicators: List<ThreatIndicator> = emptyList(), guardian: AuraGuardianAssessment? = null, file: AuraFileAnalysis? = null, vaultAvailable: Boolean = false, lanPeers: List<AuraLanPeer> = emptyList(), lastLanScan: String? = null, history: List<AuraHistoryEntry> = emptyList(), baselineTimestamp: String = "No disponible", threatSnapshot: ThreatIntelligenceSnapshot? = null): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Informe de Defensa Aura</title>
            <style>
                body { font-family: monospace; background: #050A10; color: #E8F6F3; padding: 24px; max-width: 700px; margin: 0 auto; }
                h1, h2 { color: #00E5FF; }
                h1 { border-bottom: 1px solid #00E5FF40; padding-bottom: 8px; }
                .badge { display: inline-block; background: #0E1C29; padding: 2px 8px; border: 1px solid #00E5FF; border-radius: 4px; }
                .muted { color: #A6C5C0; }
            </style>
        </head>
        <body>
            <h1>Informe Aura Defense</h1>
            <p><strong>Fecha:</strong> ${posture.timestamp}</p>
            <p><strong>Aura ID:</strong> $auraId</p>
            <p><strong>Puntuación Aura:</strong> ${if (posture.score >= 0) posture.score else "No disponible"}</p>
            <p><strong>Estado:</strong> ${posture.status}</p>

            <h2>Telemetría</h2>
            <p><strong>Fabricante:</strong> ${posture.telemetry.manufacturer}</p>
            <p><strong>Modelo:</strong> ${posture.telemetry.model}</p>
            <p><strong>Android:</strong> ${posture.telemetry.androidVersion} (API ${posture.telemetry.apiLevel})</p>
            <p><strong>Parche de seguridad:</strong> ${posture.telemetry.securityPatch}</p>
            <p><strong>Batería:</strong> ${posture.telemetry.batteryLevel}</p>
            <p><strong>Red:</strong> ${posture.telemetry.networkActive}</p>
            <p><strong>VPN:</strong> ${if (posture.telemetry.vpnActive) "Activa" else "Inactiva"}</p>
            <p><strong>DNS privado:</strong> ${posture.telemetry.privateDnsStatus}</p>

            <h2>Escáner de apps</h2>
            <p>Apps visibles: ${apps?.apps?.size ?: 0}</p>
            <p>Apps con riesgos: ${apps?.riskyApps?.size ?: 0}</p>
            <p>Riesgo alto: ${apps?.highRiskApps?.size ?: 0}</p>
            ${apps?.riskyApps?.joinToString(separator = "") { app -> "<p>- ${app.appName} (${app.packageName}): ${app.findings.joinToString(", ") { it.reason }}</p>" } ?: "<p>No se ha ejecutado un escaneo de apps</p>"}

            <h2>Análisis de enlaces</h2>
            ${if (links.isEmpty()) "<p>No se han analizado enlaces</p>" else links.joinToString(separator = "") { "<p>- ${it.risk}: ${it.url} | ${it.reasons.joinToString(", ")}</p>" }}

            <h2>Auditoría de contraseña</h2>
            <p>${password?.let { "Resultado: ${it.strength.name.lowercase(Locale.getDefault())}, puntuación ${it.score}/100." } ?: "No se ha ejecutado una auditoría"}</p>

            <h2>Protección de notificaciones</h2>
            <p>Enlaces analizados: ${notifications.size}</p>
            <p>Sospechosos: ${notifications.count { it.analysis.risk.name == "SOSPECHOSO" }}</p>
            <p>Peligrosos: ${notifications.count { it.analysis.risk.name == "PELIGROSO" }}</p>

            <h2>Inteligencia local de amenazas</h2>
            <p>Versión: ${threatSnapshot?.version ?: "local-compatible"}</p>
            <p>Última actualización: ${threatSnapshot?.updatedAt ?: indicators.maxOfOrNull { it.updatedAt } ?: "No disponible"}</p>
            <p>Fuente: ${threatSnapshot?.source ?: "Inteligencia local incluida"}</p>
            <p>Coincidencias recientes: ${countThreatMatches(links, notifications)}</p>

            <h2>Guardián Aura</h2>
            <p>Nivel: ${guardian?.level?.toSpanish() ?: "No disponible"}</p>
            <p>Confianza: ${guardian?.confidence?.toSpanish() ?: "No disponible"}</p>
            <p>Razones: ${guardian?.reasons?.joinToString("; ") ?: "No disponible"}</p>
            <p>Recomendaciones: ${guardian?.recommendations?.joinToString("; ") ?: "No disponible"}</p>

            <h2>Análisis de archivos</h2>
            <p>Archivo: ${file?.name ?: "No disponible"}</p>
            <p>Riesgo potencial: ${file?.risk ?: "No disponible"}</p>
            <p>Bóveda cifrada: ${if (vaultAvailable) "Disponible" else "No disponible"}</p>

            <h2>Historial inteligente</h2>
            <p>Eventos recientes: ${history.size}</p>
            <p>Bajos: ${history.count { it.severity == "LOW" }}</p>
            <p>Medios: ${history.count { it.severity == "MEDIUM" }}</p>
            <p>Altos: ${history.count { it.severity == "HIGH" }}</p>
            <p>Críticos: ${history.count { it.severity == "CRITICAL" }}</p>
            <p>Última línea base: $baselineTimestamp</p>

            <p class="muted">Límites reales de Android sin root: las señales disponibles dependen de la versión, permisos y fabricante. Este informe no confirma malware.</p>
        </body>
        </html>
    """.trimIndent()

    private fun StringBuilder.appendTelemetry(t: DeviceTelemetrySnapshot) {
        appendLine("Fabricante: ${t.manufacturer}")
        appendLine("Modelo: ${t.model}")
        appendLine("Android: ${t.androidVersion} (API ${t.apiLevel})")
        appendLine("Parche de seguridad: ${t.securityPatch}")
        appendLine("Batería: ${t.batteryLevel}")
        appendLine("Red: ${t.networkActive}")
        appendLine("VPN: ${if (t.vpnActive) "Activa" else "Inactiva"}")
        appendLine("DNS privado: ${t.privateDnsStatus}")
    }
}
