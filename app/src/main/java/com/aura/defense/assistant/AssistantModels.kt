package com.aura.defense.assistant

enum class AssistantResponseType {
    GENERAL_RESPONSE,
    DEVICE_ASSESSMENT,
    EVIDENCE_BASED_RECOMMENDATION
}

enum class AssistantExplanationLevel { RAPIDO, ENTENDER, TECNICO }

data class AssistantCitation(
    val source: String,
    val evidence: String,
    val observedAt: String,
    val confidence: String,
    val limitation: String,
    val ruleId: String? = null,
    val feedVersion: String? = null
)

data class AssistantProposedAction(
    val id: String,
    val title: String,
    val effect: String,
    val duration: String,
    val scope: String,
    val reversible: Boolean,
    val parameters: Map<String, String> = emptyMap()
)

data class AssistantActionResult(
    val actionId: String,
    val success: Boolean,
    val message: String,
    val status: AssistantActionStatus = if (success) AssistantActionStatus.COMPLETED else AssistantActionStatus.FAILED,
    val completedAt: Long = System.currentTimeMillis(),
    val undo: AssistantProposedAction? = null
)

enum class AssistantActionStatus {
    PENDING_REQUEST,
    COMPLETED,
    FAILED
}

data class AssistantResponse(
    val type: AssistantResponseType,
    val text: String,
    val citations: List<AssistantCitation> = emptyList(),
    val confidence: String? = null,
    val limitations: List<String> = emptyList(),
    val proposedAction: AssistantProposedAction? = null,
    val actionResult: AssistantActionResult? = null
)

data class AssistantCoverage(
    val dnsStatus: String,
    val feedStatus: String,
    val feedSource: String,
    val feedVersion: String,
    val feedUpdatedAt: String,
    val feedExpiresAt: Long?,
    val postureStatus: String,
    val postureTimestamp: String,
    val appCoverage: String,
    val privacy: String,
    val auraPermissions: Map<String, Boolean>,
    val limitations: List<String>
)

data class AssistantEvidenceContext(
    val coverage: AssistantCoverage,
    val dnsEvents: List<AssistantDnsEvent>,
    val findings: List<AssistantFinding>,
    val generatedAt: Long = System.currentTimeMillis()
)

data class AssistantDnsEvent(
    val category: String,
    val source: String,
    val feedVersion: String,
    val ruleId: String,
    val timestamp: Long
)

data class AssistantFinding(
    val title: String,
    val severity: String,
    val evidence: String,
    val timestamp: String
)

data class AssistantHistoryEntry(
    val timestamp: Long,
    val kind: String,
    val summary: String,
    val result: String
)

const val INSUFFICIENT_EVIDENCE = "No tengo evidencia suficiente para confirmar ese riesgo"