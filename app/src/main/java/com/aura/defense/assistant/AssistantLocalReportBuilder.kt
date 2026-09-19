package com.aura.defense.assistant

import android.content.Context
import com.aura.defense.security.PostureResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssistantLocalReportBuilder(
    private val context: Context,
    private val postureProvider: () -> PostureResult
) {
    fun build(): String {
        val evidence = AssistantContextBuilder(AssistantEvidenceProvider(context, postureProvider = postureProvider)).build()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        return buildString {
            appendLine("INFORME LOCAL AURA")
            appendLine("Creado: $now")
            appendLine("Cobertura actual")
            appendLine("DNS/VPN: ${evidence.coverage.dnsStatus}")
            appendLine("Feed: ${evidence.coverage.feedStatus}")
            appendLine("Fuente: ${evidence.coverage.feedSource}")
            appendLine("Versión: ${evidence.coverage.feedVersion}")
            appendLine("Actualizado: ${evidence.coverage.feedUpdatedAt}")
            appendLine("Expiración: ${evidence.coverage.feedExpiresAt ?: "No disponible"}")
            appendLine("Postura: ${evidence.coverage.postureStatus} (${evidence.coverage.postureTimestamp})")
            appendLine("Apps: ${evidence.coverage.appCoverage}")
            appendLine()
            appendLine("HALLAZGOS ABIERTOS")
            if (evidence.findings.isEmpty()) appendLine("No hay hallazgos locales disponibles.")
            evidence.findings.forEach { appendLine("- ${it.severity}: ${it.title} | ${it.evidence}") }
            appendLine()
            appendLine("EVENTOS DNS RELEVANTES")
            if (evidence.dnsEvents.isEmpty()) appendLine("No hay eventos DNS reales disponibles.")
            evidence.dnsEvents.forEach { event ->
                appendLine("- ${Date(event.timestamp)} | ${event.category} | fuente=${event.source} | versión=${event.feedVersion} | regla=${event.ruleId}")
            }
            appendLine()
            appendLine("EXCEPCIONES ACTIVAS")
            val exceptions = com.aura.defense.vpn.DnsFirewallStore(context).temporaryExceptions()
            appendLine("Cantidad: ${exceptions.size}; los dominios se omiten por privacidad.")
            appendLine()
            appendLine("PRIVACIDAD Y LÍMITES")
            appendLine(evidence.coverage.privacy)
            evidence.coverage.limitations.forEach { appendLine("- $it") }
            appendLine("Este informe permanece local y no se comparte automáticamente.")
        }
    }
}
