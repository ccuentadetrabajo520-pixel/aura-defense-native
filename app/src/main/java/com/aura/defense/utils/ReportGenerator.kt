package com.aura.defense.utils

import android.content.Context
import com.aura.defense.data.repository.SecurityRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportGenerator(context: Context) {
    private val repository = SecurityRepository(context)

    fun generateSecurityReport(): String {
        val status = repository.securityStatus.value
        val threats = repository.threats.value
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        return buildString {
            appendLine("=== AURA DEFENS - REPORTE DE SEGURIDAD ===")
            appendLine()
            appendLine("Fecha: $now")
            appendLine("Puntuacion de seguridad: ${status.score}/100")
            appendLine("Amenazas encontradas: ${status.threatsFound}")
            appendLine("Apps escaneadas: ${status.appsScanned}")
            appendLine("VPN activa: ${if (status.isVpnActive) "Si" else "No"}")
            appendLine("Modo desarrollador: ${if (status.isDeveloperModeEnabled) "Activo" else "Inactivo"}")
            appendLine("Ultimo escaneo: ${status.lastScanTime}")
            appendLine()

            if (threats.isNotEmpty()) {
                appendLine("=== LISTA DE AMENAZAS ===")
                threats.forEachIndexed { index, threat ->
                    appendLine()
                    appendLine("${index + 1}. ${threat.name}")
                    appendLine("   Severidad: ${threat.severity}")
                    appendLine("   Descripcion: ${threat.description}")
                    appendLine("   Detectado: ${threat.detectedAt}")
                }
            } else {
                appendLine("No se detectaron amenazas.")
            }

            appendLine()
            appendLine("=== RECOMENDACIONES ===")
            if (!status.isVpnActive) {
                appendLine("- Activar VPN para mayor proteccion")
            }
            if (status.isDeveloperModeEnabled) {
                appendLine("- Desactivar el modo desarrollador para mayor seguridad")
            }
            if (threats.isNotEmpty()) {
                appendLine("- Revisar y eliminar las apps sospechosas")
                appendLine("- Actualizar todas las aplicaciones")
            }
            appendLine()
            appendLine("=== FIN DEL REPORTE ===")
        }
    }
}
