package com.aura.defense.security

import com.aura.defense.data.DeviceTelemetry
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
    val telemetry: DeviceTelemetry,
    val timestamp: String
)

class SecurityPostureEngine {
    fun evaluate(telemetry: DeviceTelemetry): PostureResult {
        val findings = buildList {
            if (!telemetry.bloqueoSeguro) add(SecurityFinding("Bloqueo de pantalla", FindingSeverity.HIGH, "No hay un bloqueo seguro configurado.", "Un bloqueo seguro reduce el acceso físico no autorizado al dispositivo.", "Configura PIN, contraseña o patrón seguro en Ajustes de Android.", SettingsAction.SEGURIDAD))
            if (telemetry.adbActivo == true) add(SecurityFinding("Depuración ADB", FindingSeverity.HIGH, "La depuración ADB está activada.", "ADB permite conexiones de desarrollo que aumentan la superficie de acceso del dispositivo.", "Desactiva la depuración ADB cuando no la necesites en Ajustes de desarrollador.", SettingsAction.DESARROLLADOR))
            if (telemetry.opcionesDesarrollador == true) add(SecurityFinding("Opciones de desarrollador", FindingSeverity.MEDIUM, "Las opciones de desarrollador están activadas.", "Estas opciones son útiles para desarrollo, pero amplían los controles expuestos del sistema.", "Revísalas y desactívalas si no las utilizas.", SettingsAction.DESARROLLADOR))
            if (!telemetry.vpnActiva) add(SecurityFinding("VPN", FindingSeverity.LOW, "No se detecta una VPN activa.", "El motor no activa ni simula una VPN; solo informa del estado observado.", "Activa una VPN de confianza si necesitas proteger el tráfico en redes no confiables.", SettingsAction.VPN))
            if (telemetry.dnsPrivado == "Inactivo" || telemetry.dnsPrivado == "No disponible") add(SecurityFinding("DNS privado", FindingSeverity.LOW, "Estado observado: ${telemetry.dnsPrivado}.", "El DNS privado puede proteger las consultas cuando el proveedor lo admite.", "Revisa DNS privado en los ajustes de red de Android.", SettingsAction.RED))
            if (!telemetry.redActiva) add(SecurityFinding("Conectividad", FindingSeverity.MEDIUM, "No se detecta una red activa.", "Sin red activa no es posible confirmar el estado de conectividad validada.", "Comprueba los ajustes de red cuando necesites conectividad.", SettingsAction.RED))
            if (telemetry.redValidada == false) add(SecurityFinding("Red no validada", FindingSeverity.LOW, "Android no ha validado la red activa.", "La validación de red es una señal del sistema y puede variar según la red o el fabricante.", "Revisa la conexión y la red utilizada."))
            telemetry.bateriaPorcentaje?.let { if (it < 20) add(SecurityFinding("Batería baja", FindingSeverity.MEDIUM, "Batería al $it%.", "Una batería muy baja puede limitar servicios de protección y conectividad.", "Carga el dispositivo cuando sea posible.")) }
            telemetry.almacenamientoDisponibleBytes?.let { available ->
                telemetry.almacenamientoTotalBytes?.let { total -> if (available.toDouble() / total < 0.10) add(SecurityFinding("Almacenamiento limitado", FindingSeverity.MEDIUM, "Queda menos del 10% del almacenamiento interno.", "El espacio insuficiente puede afectar a actualizaciones y al funcionamiento del sistema.", "Libera espacio desde los ajustes de almacenamiento.")) }
            }
            telemetry.ramDisponibleBytes?.let { available ->
                telemetry.ramTotalBytes?.let { total -> if (available.toDouble() / total < 0.10) add(SecurityFinding("RAM disponible limitada", FindingSeverity.LOW, "Queda menos del 10% de RAM disponible.", "La presión de memoria puede afectar a la estabilidad de procesos activos.", "Cierra procesos que no necesites y revisa las apps activas.")) }
            }
            val patchAge = patchAgeInDays(telemetry.parcheSeguridad)
            if (patchAge != null && patchAge > 180) add(SecurityFinding("Parche de seguridad antiguo", FindingSeverity.MEDIUM, "El parche tiene aproximadamente $patchAge días.", "Los parches del sistema corrigen problemas conocidos de seguridad y estabilidad.", "Busca actualizaciones del sistema en Ajustes de Android.", SettingsAction.SEGURIDAD))
            if (telemetry.api < 29) add(SecurityFinding("Versión de Android antigua", FindingSeverity.MEDIUM, "API ${telemetry.api} (${telemetry.versionAndroid}).", "Las versiones antiguas pueden no incluir controles modernos del sistema.", "Comprueba si hay una actualización disponible.", SettingsAction.SEGURIDAD))
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
