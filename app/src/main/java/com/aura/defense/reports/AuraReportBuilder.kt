package com.aura.defense.reports

import com.aura.defense.apps.AppScanResult
import com.aura.defense.data.DeviceTelemetrySnapshot
import com.aura.defense.security.PostureResult
import com.aura.defense.tools.LinkAnalysis
import com.aura.defense.tools.PasswordAudit
import com.aura.defense.notifications.NotificationAlert
import java.util.Locale

class AuraReportBuilder {
    fun text(auraId: String, posture: PostureResult, apps: AppScanResult?, links: List<LinkAnalysis>, password: PasswordAudit?, notifications: List<NotificationAlert> = emptyList()): String = buildString {
        appendLine("INFORME AURA DEFENSE")
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
        appendLine("Límites reales de Android sin root: las señales disponibles dependen de la versión, permisos y fabricante. Este informe no confirma malware.")
    }

    fun json(auraId: String, posture: PostureResult, apps: AppScanResult?, links: List<LinkAnalysis>, password: PasswordAudit?, notifications: List<NotificationAlert> = emptyList()): String {
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
            "\"auditoriaContrasena\":${password?.let { quote(it.strength.name) } ?: "null"}",
            "\"limitesAndroid\":${quote("Las señales dependen de la versión, permisos y fabricante; no confirma malware")}" 
        ).joinToString(",") + "}"
    }

    private fun StringBuilder.appendNotificationSummary(alerts: List<NotificationAlert>) {
        appendLine("Enlaces analizados: ${alerts.size}")
        appendLine("Sospechosos: ${alerts.count { it.analysis.risk.name == "SOSPECHOSO" }}")
        appendLine("Peligrosos: ${alerts.count { it.analysis.risk.name == "PELIGROSO" }}")
    }

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
