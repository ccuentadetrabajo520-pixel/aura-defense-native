package com.aura.defense.history

import com.aura.defense.apps.AppScanResult
import com.aura.defense.security.PostureResult
import com.aura.defense.tools.LinkAnalysis
import com.aura.defense.tools.LinkRisk
import com.aura.defense.notifications.NotificationAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SuspiciousChangeDetector(private val store: AuraHistoryStore) {
    data class Result(val entries: List<AuraHistoryEntry>, val baselineCreated: Boolean)

    fun compare(posture: PostureResult, appScan: AppScanResult?, links: List<LinkAnalysis>, notificationAlerts: List<NotificationAlert> = emptyList()): Result {
        val current = AuraBaseline.from(posture, appScan)
        val previous = store.baseline()
        if (previous == null) {
            return Result(emptyList(), store.saveBaseline(current))
        }
        val now = timestamp()
        val events = mutableListOf<AuraHistoryEntry>()
        val previousPackages = previous.apps.keys
        val currentPackages = current.apps.keys
        (currentPackages - previousPackages).take(20).forEach { packageName ->
            val currentApp = current.apps.getValue(packageName)
            val unknownInstaller = currentApp.installer.isBlank() || currentApp.installer == "Desconocido"
            events.add(event(now, "NUEVA_APP", if (unknownInstaller) "MEDIUM" else "LOW", "Nueva app visible", "Se detectó una app que no estaba en la línea base.", packageName, "Revisa la app si no reconoces su instalación.", "Escáner de apps"))
            if (unknownInstaller) events.add(event(now, "INSTALADOR_DESCONOCIDO", "MEDIUM", "Instalador desconocido", "La app nueva no identifica un instalador conocido.", packageName, "Revisión recomendada.", "Escáner de apps"))
        }
        (previousPackages - currentPackages).take(20).forEach { packageName ->
            events.add(event(now, "APP_REMOVIDA", "LOW", "App removida", "Una app de la línea base ya no está visible.", packageName, "Comprueba que la eliminación haya sido intencionada.", "Escáner de apps"))
        }
        currentPackages.intersect(previousPackages).take(50).forEach { packageName ->
            val old = previous.apps.getValue(packageName)
            val currentApp = current.apps.getValue(packageName)
            if (old.versionName != currentApp.versionName) events.add(event(now, "APP_ACTUALIZADA", "LOW", "App actualizada", "Cambió la versión de una app visible.", packageName, "Revisa la actualización si no la esperabas.", "Escáner de apps"))
            if (currentApp.targetSdk != old.targetSdk) events.add(event(now, "SDK_CAMBIADO", "MEDIUM", "Configuración de app cambiada", "Cambió el SDK objetivo de una app visible.", packageName, "Revisión recomendada.", "Escáner de apps"))
            if ((currentApp.permissions.toSet() - old.permissions.toSet()).isNotEmpty()) events.add(event(now, "PERMISO_SENSIBLE", "MEDIUM", "Permiso sensible detectado", "Una app solicita permisos adicionales desde la línea base.", packageName, "Revisa los permisos concedidos.", "Escáner de apps"))
            if (old.installer != "Desconocido" && currentApp.installer == "Desconocido") events.add(event(now, "INSTALADOR_DESCONOCIDO", "MEDIUM", "Instalador desconocido", "Una app visible ya no identifica un instalador conocido.", packageName, "Revisión recomendada.", "Escáner de apps"))
            if (currentApp.findings.contains("HIGH") || currentApp.findings.contains("CRITICAL")) {
                if (!old.findings.contains("HIGH") && !old.findings.contains("CRITICAL")) events.add(event(now, "RIESGO_APP", "HIGH", "Nueva señal de riesgo alto", "Una app presenta una señal de riesgo alto nueva.", packageName, "Revisa los detalles de la app.", "Escáner de apps"))
            }
        }
        if (previous.vpnActive && !current.vpnActive) events.add(event(now, "VPN_CAMBIO", "HIGH", "VPN desactivada", "La VPN estaba activa en la línea base y ahora no se detecta.", "Estado observado del dispositivo.", "Revisa los ajustes de VPN.", "Telemetría"))
        if (previous.privateDnsStatus != current.privateDnsStatus && previous.privateDnsStatus == "Activo") events.add(event(now, "DNS_CAMBIO", "HIGH", "DNS privado cambió", "El DNS privado activo en la línea base ya no tiene el mismo estado.", current.privateDnsStatus, "Revisa los ajustes de red.", "Telemetría"))
        if (previous.score >= 0 && current.score >= 0 && previous.score - current.score >= 20) events.add(event(now, "PUNTUACION_BAJA", "HIGH", "La puntuación Aura bajó", "La puntuación disminuyó significativamente respecto a la línea base.", "Antes: ${previous.score}; ahora: ${current.score}.", "Ejecuta una revisión recomendada.", "Diagnóstico"))
        (links + notificationAlerts.map { it.analysis }).filter { it.risk == LinkRisk.PELIGROSO && it.reasons.contains("Coincidencia en inteligencia local") }.take(10).forEach { analysis ->
            events.add(event(now, "INTELIGENCIA_MATCH", "CRITICAL", "Coincidencia crítica en inteligencia local", "Un enlace reciente coincide con inteligencia local crítica.", analysis.url, "No abras el enlace y revisa su origen.", "Inteligencia local"))
        }
        if (events.isNotEmpty()) store.addEntries(events)
        store.saveBaseline(current)
        return Result(events, false)
    }

    private fun event(timestamp: String, type: String, severity: String, title: String, description: String, evidence: String, action: String, source: String) = AuraHistoryEntry(timestamp, type, severity, title, description, evidence, action, source)
    private fun timestamp() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
}
