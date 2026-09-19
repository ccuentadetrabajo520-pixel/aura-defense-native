package com.aura.defense.assistant

import android.content.Context
import com.aura.defense.vpn.DnsFirewallStore
import java.util.concurrent.ConcurrentHashMap

enum class AssistantToolKind { READ_ONLY, CONFIRMATION_REQUIRED }

data class AssistantTool(val id: String, val kind: AssistantToolKind, val description: String)

class AssistantToolRegistry {
    val tools = listOf(
        AssistantTool("get_coverage", AssistantToolKind.READ_ONLY, "Consultar cobertura actual"),
        AssistantTool("get_feed_status", AssistantToolKind.READ_ONLY, "Consultar estado del feed firmado"),
        AssistantTool("get_dns_events", AssistantToolKind.READ_ONLY, "Mostrar eventos DNS reales"),
        AssistantTool("get_findings", AssistantToolKind.READ_ONLY, "Mostrar hallazgos locales"),
        AssistantTool("get_posture", AssistantToolKind.READ_ONLY, "Mostrar postura disponible"),
        AssistantTool("get_alert_evidence", AssistantToolKind.READ_ONLY, "Mostrar evidencia de una alerta"),
        AssistantTool("start_dns", AssistantToolKind.CONFIRMATION_REQUIRED, "Iniciar protección DNS"),
        AssistantTool("stop_dns", AssistantToolKind.CONFIRMATION_REQUIRED, "Detener protección DNS"),
        AssistantTool("create_temporary_exception", AssistantToolKind.CONFIRMATION_REQUIRED, "Crear excepción temporal"),
        AssistantTool("remove_temporary_exception", AssistantToolKind.CONFIRMATION_REQUIRED, "Retirar excepción"),
        AssistantTool("clear_local_activity", AssistantToolKind.CONFIRMATION_REQUIRED, "Borrar actividad local"),
        AssistantTool("clear_assistant_history", AssistantToolKind.CONFIRMATION_REQUIRED, "Borrar historial del asistente"),
        AssistantTool("generate_local_report", AssistantToolKind.CONFIRMATION_REQUIRED, "Generar informe local")
    )

    fun find(id: String): AssistantTool? = tools.firstOrNull { it.id == id }
}

class AssistantActionPolicy(private val registry: AssistantToolRegistry = AssistantToolRegistry()) {
    private val confirmed = ConcurrentHashMap.newKeySet<String>()
    private val lastRun = ConcurrentHashMap<String, Long>()

    fun request(action: AssistantProposedAction): AssistantResponse {
        val tool = registry.find(action.id)
        if (tool == null || tool.kind != AssistantToolKind.CONFIRMATION_REQUIRED) {
            return AssistantResponse(AssistantResponseType.EVIDENCE_BASED_RECOMMENDATION, "La acción solicitada no está disponible y no se ejecutó.")
        }
        return AssistantResponse(
        AssistantResponseType.EVIDENCE_BASED_RECOMMENDATION,
        "Acción propuesta: ${action.title}. Efecto: ${action.effect}. Duración: ${action.duration}. Alcance: ${action.scope}. ${if (action.reversible) "Se puede revertir." else "Puede requerir pasos manuales para revertirse."} Confirma explícitamente para ejecutarla.",
        proposedAction = action
        )
    }

    fun confirm(action: AssistantProposedAction): Boolean = registry.find(action.id)?.kind == AssistantToolKind.CONFIRMATION_REQUIRED && confirmed.add(key(action))

    fun consume(action: AssistantProposedAction): Boolean {
        if (!confirmed.remove(key(action))) return false
        val now = System.currentTimeMillis()
        val previous = lastRun.put(key(action), now)
        return previous == null || now - previous >= RATE_LIMIT_MS
    }

    fun validateDomain(value: String): String? = value.trim().lowercase().takeIf {
        it.length <= 253 && it.isNotBlank() && it.all { char -> char.isLetterOrDigit() || char == '.' || char == '-' }
    }

    private fun key(action: AssistantProposedAction) = action.id + action.parameters.toSortedMap().toString()
    private companion object { const val RATE_LIMIT_MS = 2_000L }
}

class AssistantActionExecutor(
    private val context: Context,
    private val postureProvider: () -> com.aura.defense.security.PostureResult = { com.aura.defense.security.PostureResult.pending() },
    private val registry: AssistantToolRegistry = AssistantToolRegistry()
) {
    fun execute(action: AssistantProposedAction): AssistantActionResult {
        if (registry.find(action.id) == null) return AssistantActionResult(action.id, false, "La herramienta no está registrada; no se ejecutó.")
        val store = DnsFirewallStore(context)
        return when (action.id) {
            "clear_local_activity" -> {
                val cleared = store.clearActivity()
                AssistantActionResult(action.id, cleared, if (cleared) "Actividad local borrada." else "No se pudo borrar la actividad.")
            }
            "create_temporary_exception" -> {
                val domain = action.parameters["domain"]?.let { AssistantActionPolicy().validateDomain(it) }
                val minutes = action.parameters["minutes"]?.toLongOrNull()?.coerceIn(1, 1440)
                if (domain == null || minutes == null) AssistantActionResult(action.id, false, "Parámetros inválidos; no se ejecutó la acción.")
                else {
                    val success = store.allowTemporarily(domain, minutes * 60_000L, "Excepción confirmada por el usuario")
                    AssistantActionResult(action.id, success, if (success) "Excepción temporal creada para $domain durante $minutes minutos." else "No se pudo crear la excepción temporal.")
                }
            }
            "remove_temporary_exception" -> {
                val domain = action.parameters["domain"]?.let { AssistantActionPolicy().validateDomain(it) }
                if (domain == null) AssistantActionResult(action.id, false, "Dominio inválido; no se ejecutó la acción.")
                else {
                    val success = store.removeTemporaryException(domain)
                    AssistantActionResult(action.id, success, if (success) "Excepción retirada para $domain." else "No se pudo retirar la excepción.")
                }
            }
            "clear_assistant_history" -> {
                val cleared = AssistantHistoryRepository(context).clear()
                AssistantActionResult(action.id, cleared, if (cleared) "Historial del asistente borrado." else "No se pudo borrar el historial.")
            }
            "generate_local_report" -> {
                val file = java.io.File(context.filesDir, "aura_assistant_report.txt")
                val saved = runCatching { file.writeText(AssistantLocalReportBuilder(context, postureProvider).build()); true }.getOrDefault(false)
                AssistantActionResult(action.id, saved, if (saved) "Informe local generado en almacenamiento privado." else "No se pudo generar el informe local.")
            }
            else -> AssistantActionResult(action.id, false, "La acción requiere un adaptador de UI o servicio real; no se ejecutó.")
        }
    }
}