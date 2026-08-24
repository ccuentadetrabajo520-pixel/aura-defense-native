package com.aura.defense.security

import com.aura.defense.data.DeviceTelemetrySnapshot
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class FindingSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class SecurityFinding(
    val title: String,
    val severity: FindingSeverity,
    val evidence: String,
    val explanation: String,
    val recommendedAction: String,
    val settingsAction: SettingsAction? = null
)

enum class SettingsAction { VPN, SEGURIDAD, DESARROLLADOR, RED, NOTIFICACIONES }

data class PostureResult(
    val score: Int,
    val status: String,
    val findings: List<SecurityFinding>,
    val telemetry: DeviceTelemetrySnapshot,
    val timestamp: String
) {
    companion object {
        fun pending() = PostureResult(
            score = -1,
            status = "Diagnóstico pendiente",
            findings = emptyList(),
            telemetry = DeviceTelemetrySnapshot(
                manufacturer = "No disponible",
                model = "No disponible",
                androidVersion = "No disponible",
                apiLevel = 0,
                securityPatch = "No disponible",
                batteryLevel = "No disponible",
                ramAvailableBytes = 0L,
                ramTotalBytes = 0L,
                storageAvailableBytes = 0L,
                storageTotalBytes = 0L,
                networkActive = "No disponible",
                vpnActive = false,
                privateDnsStatus = "No disponible"
            ),
            timestamp = "No disponible"
        )
    }
}

class SecurityPostureEngine {
    fun evaluate(telemetry: DeviceTelemetrySnapshot, riskyApps: Int = 0, highRiskApps: Int = 0): PostureResult {
        val findings = buildList {
            if (!telemetry.batteryLevel.contains("%")) add(SecurityFinding("Batería no disponible", FindingSeverity.LOW, "No se pudo leer el nivel de batería.", "Esta señal no está disponible en este dispositivo.", "Vuelve a ejecutar el diagnóstico más tarde."))
            if (!telemetry.vpnActive) add(SecurityFinding("VPN", FindingSeverity.LOW, "No se detecta una VPN activa.", "El motor solo informa del estado observado.", "Activa una VPN de confianza si necesitas proteger el tráfico.", SettingsAction.VPN))
            if (telemetry.privateDnsStatus == "Inactivo" || telemetry.privateDnsStatus == "No disponible") add(SecurityFinding("DNS privado", FindingSeverity.LOW, "Estado observado: ${telemetry.privateDnsStatus}.", "El DNS privado puede proteger las consultas cuando el sistema lo admite.", "Revisa DNS privado en los ajustes de red de Android.", SettingsAction.RED))
            if (telemetry.networkActive == "No disponible") add(SecurityFinding("Conectividad", FindingSeverity.MEDIUM, "No se detecta una red activa.", "No es posible confirmar la conectividad actual.", "Comprueba los ajustes de red.", SettingsAction.RED))
            val patchAge = patchAgeInDays(telemetry.securityPatch)
            if (patchAge != null && patchAge > 180) add(SecurityFinding("Parche de seguridad antiguo", FindingSeverity.MEDIUM, "El parche tiene aproximadamente $patchAge días.", "Los parches del sistema corrigen problemas conocidos de seguridad y estabilidad.", "Busca actualizaciones del sistema en Ajustes de Android.", SettingsAction.SEGURIDAD))
            if (telemetry.apiLevel in 1..28) add(SecurityFinding("Versión de Android antigua", FindingSeverity.MEDIUM, "API ${telemetry.apiLevel} (${telemetry.androidVersion}).", "Las versiones antiguas pueden no incluir controles modernos del sistema.", "Comprueba si hay una actualización disponible.", SettingsAction.SEGURIDAD))
            if (riskyApps > 0) add(SecurityFinding("Apps con señales de riesgo", if (highRiskApps > 0) FindingSeverity.HIGH else FindingSeverity.MEDIUM, "${riskyApps} apps requieren revisión; ${highRiskApps} tienen severidad alta o crítica.", "El escáner ha encontrado permisos o configuraciones sensibles en apps visibles por Android.", "Revisa los detalles de las apps detectadas."))
        }
        val score = (100 - findings.sumOf { severityPenalty(it.severity) }).coerceIn(0, 100)
        return PostureResult(score, statusFor(score), findings, telemetry, java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(java.util.Date()))
    }

    private fun severityPenalty(severity: FindingSeverity) = when (severity) {
        FindingSeverity.LOW -> 4
        FindingSeverity.MEDIUM -> 9
        FindingSeverity.HIGH -> 18
        FindingSeverity.CRITICAL -> 28
    }

    private fun statusFor(score: Int) = when {
        score >= 85 -> "Protegido"
        score >= 60 -> "Parcial"
        else -> "Riesgo alto"
    }

    private fun patchAgeInDays(value: String): Long? = runCatching {
        if (value == "No disponible") return null
        val date = LocalDate.parse(value, DateTimeFormatter.ISO_DATE)
        Period.between(date, LocalDate.now()).days.toLong() + Period.between(date, LocalDate.now()).months * 30L + Period.between(date, LocalDate.now()).years * 365L
    }.getOrNull()
}
