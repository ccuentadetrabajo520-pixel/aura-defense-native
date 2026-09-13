package com.aura.defense.ai

import android.content.Context
import com.aura.defense.security.PostureResult
import com.aura.defense.vpn.DnsFirewallStore
import com.aura.defense.vpn.ThreatFeedManager
import com.aura.defense.threats.ThreatIntelligenceEngine


data class CopilotResponse(
    val text: String,
    val isEducational: Boolean,
    val needsConfirmation: Boolean? = null,
    val pendingAction: String? = null
)

class CopilotBrain(
    private val context: Context,
    private val postureProvider: () -> PostureResult,
    private val vpnRunningProvider: () -> Boolean,
    private val threatEngine: ThreatIntelligenceEngine?
) {
    var pendingAction: String? = null
        private set

    fun process(input: String): CopilotResponse {
        val q = SecurityKnowledgeBase.normalize(input.trim())
        if (q.isBlank()) return CopilotResponse("Escribe o di algo y te ayudo.", false)

        pendingAction?.let { action ->
            val tokens = q.split(Regex("\\s+"))
            val yes = tokens.any { it in setOf("si", "dale", "ok", "confirmo", "hazlo", "aceptar") }
            val no = tokens.any { it in setOf("no", "cancela", "espera", "detente") }
            if (yes) {
                pendingAction = null
                return CopilotResponse(executeAction(action), false, false, action)
            }
            if (no) {
                pendingAction = null
                return CopilotResponse("Hecho, no hice nada. Tu decisión.", false, false)
            }
            return CopilotResponse("No entendí si confirmas. Dime 'sí' o 'no'.", false, true, action)
        }

        if (q.contains("score") || q.contains("puntuac")) return CopilotResponse(scoreAnswer(), false)
        if (q.contains("red") || q.contains("wifi")) return CopilotResponse(networkAnswer(), false)
        if (q.contains("bloque") && listOf("cuant", "list", "hoy").any { q.contains(it) }) {
            return CopilotResponse(dnsAnswer(), false)
        }

        if (q.contains("vpn") && listOf("activa", "enciende", "prende", "desactiv", "apaga", "quita").any { q.contains(it) }) {
            val turningOn = listOf("desactiv", "apaga", "quita").none { q.contains(it) }
            if (turningOn && vpnRunningProvider()) return CopilotResponse("El túnel ya está activo.", false)
            if (!turningOn && !vpnRunningProvider()) return CopilotResponse("El túnel ya está inactivo.", false)
            pendingAction = if (turningOn) "ACTIVATE_VPN" else "DEACTIVATE_VPN"
            val message = if (turningOn) "Voy a activar el túnel VPN. ¿Confirmas?" else "Voy a desactivar el túnel VPN. ¿Confirmas?"
            return CopilotResponse(message, false, true, pendingAction)
        }

        if (q.contains("estricto") || q.contains("equilibrado") || q.contains("permitir todo")) {
            val profile = when {
                q.contains("estricto") -> "ESTRICTO"
                q.contains("equilibrado") -> "EQUILIBRADO"
                else -> "PERMITIR_TODO"
            }
            pendingAction = "SET_PROFILE:$profile"
            return CopilotResponse("Voy a cambiar el cortafuegos al perfil $profile. ¿Confirmas?", false, true, pendingAction)
        }

        if (q.contains("escane")) {
            pendingAction = "RUN_SCAN"
            return CopilotResponse("Voy a escanear todas tus aplicaciones. ¿Confirmas?", false, true, pendingAction)
        }

        val domainRegex = Regex("""(?:(?:[a-z0-9-]+)\.)+(?:com|net|org|es|mx|io|xyz|top|online|site|info|co|app|link|ru|su|shop|cl|ar|pe|ve)""")
        domainRegex.find(q)?.let { match ->
            val domain = match.value
            val category = ThreatFeedManager.categoryOf(domain)
            val verdict = when {
                category != null -> "⚠ BLOQUEADO por AURA - categoría: $category. NO lo abras."
                threatEngine?.findMatches(domain)?.isNotEmpty() == true -> "⚠ En inteligencia local. NO lo abras."
                else -> "No aparece en mis bases actuales. Eso NO garantiza que sea seguro; verifica el remitente por canales oficiales."
            }
            return CopilotResponse("Consulté $domain en mis bases: $verdict", false)
        }

        SecurityKnowledgeBase.search(input)?.let { entry ->
            return CopilotResponse(
                "${entry.title}: ${entry.shortAnswer}\n\n${entry.detail}\n\n🛡 Cómo te proteges: ${entry.protectionTip}",
                true
            )
        }

        return CopilotResponse(
            "Eso está fuera de lo que puedo ver o explicar con certeza, y prefiero admitirlo a inventar. Puedo explicar conceptos de seguridad, consultar dominios, decirte tu score y estado de red, o ejecutar acciones confirmadas.",
            false
        )
    }

    private fun executeAction(action: String): String = when {
        action == "ACTIVATE_VPN" -> "Activando el túnel VPN."
        action == "DEACTIVATE_VPN" -> "Desactivando el túnel VPN."
        action.startsWith("SET_PROFILE:") -> "Perfil del cortafuegos cambiado a ${action.substringAfter(':')}."
        action == "RUN_SCAN" -> "Iniciando escaneo real de aplicaciones."
        else -> "Acción desconocida."
    }

    private fun scoreAnswer(): String {
        val posture = postureProvider()
        return if (posture.score < 0) {
            "El diagnóstico aún no se ha ejecutado. Pídeme que escanee tus apps o pulsa SCAN."
        } else {
            "Tu score es ${posture.score}/100 (${posture.status}). Tiene ${posture.findings.size} hallazgo(s) reales: " +
                posture.findings.take(4).joinToString("; ") { "${it.title} (${it.severity})" } + "."
        }
    }

    private fun networkAnswer(): String {
        val posture = postureProvider()
        return "Red activa: ${posture.telemetry.networkActive}. VPN: ${if (posture.telemetry.vpnActive) "activa" else "inactiva"}. DNS privado: ${posture.telemetry.privateDnsStatus}. Feeds de inteligencia: ${ThreatFeedManager.size()} dominios cargados."
    }

    private fun dnsAnswer(): String {
        val store = DnsFirewallStore(context)
        return "El cortafuegos bloqueó ${store.blockedCount()} dominios en total. " +
            (store.blockedEvents().takeLast(3).takeIf { it.isNotEmpty() }
                ?.joinToString("; ") { "${it.domain} [${it.category}]" }
                ?: "Sin bloqueos recientes en esta sesión.")
    }
}
