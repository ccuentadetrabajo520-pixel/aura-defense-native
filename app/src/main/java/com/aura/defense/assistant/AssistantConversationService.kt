package com.aura.defense.assistant

import android.content.Context
import com.aura.defense.security.PostureResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssistantConversationService(
    context: Context,
    postureProvider: () -> PostureResult,
    private val model: AssistantModelProvider = LocalAssistantModelProvider(),
    private val history: AssistantHistoryRepository = AssistantHistoryRepository(context),
    private val actionExecutor: (AssistantProposedAction) -> AssistantActionResult = AssistantActionExecutor(context, postureProvider)::execute,
    private val toolRegistry: AssistantToolRegistry = AssistantToolRegistry()
) {
    private val contextBuilder = AssistantContextBuilder(AssistantEvidenceProvider(context, postureProvider = postureProvider))
    private val policy = AssistantActionPolicy(toolRegistry)

    suspend fun respond(input: String, level: AssistantExplanationLevel = AssistantExplanationLevel.ENTENDER): AssistantResponse {
        val normalized = input.trim().lowercase()
        val response = when {
            AssistantInputSafety.isUntrustedInstruction(normalized) -> model.respond(input, level)
            isAssessmentRequest(normalized) -> assess(level)
            normalized.contains("activa") && normalized.contains("dns") -> policy.request(
                AssistantProposedAction("start_dns", "Iniciar protección DNS", "solicitará el permiso VPN si hace falta", "hasta que la detengas", "consultas DNS observadas", true)
            )
            (normalized.contains("detén") || normalized.contains("deten") || normalized.contains("apaga")) && normalized.contains("dns") -> policy.request(
                AssistantProposedAction("stop_dns", "Detener protección DNS", "detendrá el servicio DNS local", "inmediata", "protección DNS de AURA", true)
            )
            normalized.contains("retirar") && normalized.contains("excep") -> removeExceptionProposal(normalized)
            normalized.contains("permitir") || normalized.contains("crear excepcion") || normalized.contains("crear excepción") -> temporaryExceptionProposal(normalized)
            normalized.contains("borrar") && normalized.contains("historial") -> policy.request(
                AssistantProposedAction("clear_assistant_history", "Borrar historial del asistente", "eliminará conversaciones y acciones locales", "permanente", "memoria conversacional de AURA", true)
            )
            normalized.contains("borrar") && normalized.contains("actividad") -> policy.request(
                AssistantProposedAction("clear_local_activity", "Borrar actividad local", "eliminará eventos DNS conservados", "permanente", "actividad local de AURA", false)
            )
            normalized.contains("generar") && normalized.contains("informe") -> policy.request(
                AssistantProposedAction("generate_local_report", "Generar informe local", "escribirá un informe redactado en almacenamiento privado", "hasta que lo borres", "estado local disponible", true)
            )
            else -> model.respond(input, level)
        }
        history.add(AssistantHistoryEntry(System.currentTimeMillis(), response.type.name, response.text, response.actionResult?.message ?: "propuesta/no acción"))
        return response
    }

    fun confirm(action: AssistantProposedAction): AssistantResponse {
        if (!policy.confirm(action)) return AssistantResponse(AssistantResponseType.EVIDENCE_BASED_RECOMMENDATION, "La confirmación no es válida.")
        if (!policy.consume(action)) return AssistantResponse(AssistantResponseType.EVIDENCE_BASED_RECOMMENDATION, "Acción limitada temporalmente; no se ejecutó.")
        val result = actionExecutor(action)
        if (action.id != "clear_assistant_history") {
            history.add(AssistantHistoryEntry(result.completedAt, "ACTION", action.id, result.message))
        }
        AssistantTimelineStore(context).record(
            kind = "USER_ACTION",
            source = "AssistantActionPolicy",
            evidence = "acción=${action.id}; alcance=${action.scope}; duración=${action.duration}",
            result = result.message,
            timestamp = result.completedAt
        )
        return AssistantResponse(AssistantResponseType.EVIDENCE_BASED_RECOMMENDATION, result.message, actionResult = result)
    }

    fun propose(action: AssistantProposedAction): AssistantResponse = policy.request(action)
    fun clearHistory(): Boolean = history.clear()
    fun historyEntries(): List<AssistantHistoryEntry> = history.entries()

    private fun assess(level: AssistantExplanationLevel): AssistantResponse {
        val evidence = contextBuilder.build()
        val citation = AssistantCitation(
            source = "AURA local",
            evidence = "DNS=${evidence.coverage.dnsStatus}; feed=${evidence.coverage.feedStatus}",
            observedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(evidence.generatedAt)),
            confidence = if (evidence.coverage.feedStatus == "CURRENT") "Alta para el estado observado" else "Limitada",
            limitation = evidence.coverage.limitations.first(),
            feedVersion = evidence.coverage.feedVersion.takeUnless { it == "No disponible" }
        )
        val text = when (level) {
            AssistantExplanationLevel.RAPIDO -> "Cobertura actual: DNS ${evidence.coverage.dnsStatus}; feed ${evidence.coverage.feedStatus}."
            AssistantExplanationLevel.ENTENDER -> "Hecho observado: DNS ${evidence.coverage.dnsStatus} y feed ${evidence.coverage.feedStatus}. Señales disponibles: ${evidence.findings.size}. Recomendación: revisa los detalles y límites antes de decidir."
            AssistantExplanationLevel.TECNICO -> "Cobertura actual: DNS=${evidence.coverage.dnsStatus}, feed=${evidence.coverage.feedSource} ${evidence.coverage.feedVersion}, postura=${evidence.coverage.postureStatus}, tomada=${evidence.coverage.postureTimestamp}."
        }
        return AssistantResponse(AssistantResponseType.DEVICE_ASSESSMENT, text, listOf(citation), "Media", evidence.coverage.limitations)
    }

    private fun isAssessmentRequest(value: String): Boolean = listOf("evalua", "diagnost", "cobertura actual", "estado del telefono", "estado del teléfono", "evidencia local").any(value::contains)

    private fun temporaryExceptionProposal(value: String): AssistantResponse {
        val match = Regex("(?:permitir|excepcion|excepción)\\s+([a-z0-9.-]+)(?:\\s+durante\\s+(\\d+)\\s*(?:min|minutos)?)?").find(value)
            ?: return AssistantResponse(AssistantResponseType.GENERAL_RESPONSE, "Indica un dominio válido y una duración para proponer una excepción.")
        val domain = policy.validateDomain(match.groupValues[1])
            ?: return AssistantResponse(AssistantResponseType.GENERAL_RESPONSE, "El dominio no es válido; no se propuso ninguna acción.")
        val minutes = (match.groupValues.getOrNull(2)?.toLongOrNull() ?: 15L).coerceIn(1L, 1440L)
        return policy.request(
            AssistantProposedAction(
                "create_temporary_exception",
                "Crear excepción temporal para $domain",
                "permitirá el dominio en las decisiones locales",
                "$minutes minutos",
                "excepción DNS local",
                true,
                mapOf("domain" to domain, "minutes" to minutes.toString())
            )
        )
    }

    private fun removeExceptionProposal(value: String): AssistantResponse {
        val domain = value.substringAfterLast(' ').trim().let(policy::validateDomain)
            ?: return AssistantResponse(AssistantResponseType.GENERAL_RESPONSE, "Indica un dominio válido; no se propuso ninguna acción.")
        return policy.request(
            AssistantProposedAction(
                "remove_temporary_exception",
                "Retirar excepción de $domain",
                "volverá a aplicar las decisiones DNS locales",
                "inmediata",
                "excepción DNS local",
                true,
                mapOf("domain" to domain)
            )
        )
    }

}