package com.aura.defense.assistant

import com.aura.defense.vpn.DnsProtectionState
import com.aura.defense.vpn.DnsProtectionStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class AssistantDnsActionCoordinator(
    private val state: StateFlow<DnsProtectionState>,
    private val request: (AssistantProposedAction) -> Unit,
    private val timeoutMs: Long = 15_000L
) {
    suspend fun execute(action: AssistantProposedAction): AssistantActionResult {
        val before = state.value.status
        request(action)
        val terminal = withTimeoutOrNull(timeoutMs) {
            state.first { current -> current.status in TERMINAL && current.status != before }
        }
        if (terminal == null) return AssistantActionResult(
            actionId = action.id,
            success = false,
            message = "Solicitud enviada; Android aún no confirmó el estado.",
            status = AssistantActionStatus.PENDING_REQUEST
        )
        val expected = if (action.id == "start_dns") DnsProtectionStatus.ACTIVE_DNS_ONLY else DnsProtectionStatus.OFF
        val success = terminal.status == expected
        return AssistantActionResult(
            actionId = action.id,
            success = success,
            message = terminal.detail.ifBlank { "Estado confirmado: ${terminal.status}." },
            status = if (success) AssistantActionStatus.COMPLETED else AssistantActionStatus.FAILED,
            completedAt = terminal.changedAt
        )
    }

    private companion object {
        val TERMINAL = setOf(DnsProtectionStatus.ACTIVE_DNS_ONLY, DnsProtectionStatus.OFF, DnsProtectionStatus.DEGRADED, DnsProtectionStatus.ERROR)
    }
}