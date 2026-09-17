package com.aura.defense.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DnsProtectionStatus {
    OFF,
    REQUESTING_PERMISSION,
    STARTING,
    ACTIVE_DNS_ONLY,
    DEGRADED,
    STOPPING,
    ERROR
}

enum class DnsDegradedReason {
    NONE,
    FEED_INVALID,
    UPSTREAM_UNAVAILABLE,
    DNS_PARSE_FAILURE,
    NETWORK_UNAVAILABLE,
    VPN_REVOKED,
    INTERNAL_FAILURE,
    BATTERY_RESTRICTED
}

data class DnsProtectionState(
    val status: DnsProtectionStatus,
    val reason: DnsDegradedReason = DnsDegradedReason.NONE,
    val detail: String = "",
    val version: Long = 0L,
    val changedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun off() = DnsProtectionState(DnsProtectionStatus.OFF)
    }
}

class DnsProtectionStateMachine(initial: DnsProtectionState = DnsProtectionState.off()) {
    var state: DnsProtectionState = initial
        private set

    fun transition(
        next: DnsProtectionStatus,
        reason: DnsDegradedReason = DnsDegradedReason.NONE,
        detail: String = ""
    ): Boolean {
        if (next !in VALID_TRANSITIONS[state.status].orEmpty()) return false
        state = DnsProtectionState(
            status = next,
            reason = reason,
            detail = detail,
            version = state.version + 1
        )
        return true
    }

    private companion object {
        val VALID_TRANSITIONS = mapOf(
            DnsProtectionStatus.OFF to setOf(DnsProtectionStatus.REQUESTING_PERMISSION, DnsProtectionStatus.STARTING),
            DnsProtectionStatus.REQUESTING_PERMISSION to setOf(DnsProtectionStatus.STARTING, DnsProtectionStatus.OFF, DnsProtectionStatus.ERROR),
            DnsProtectionStatus.STARTING to setOf(DnsProtectionStatus.ACTIVE_DNS_ONLY, DnsProtectionStatus.DEGRADED, DnsProtectionStatus.ERROR, DnsProtectionStatus.STOPPING),
            DnsProtectionStatus.ACTIVE_DNS_ONLY to setOf(DnsProtectionStatus.DEGRADED, DnsProtectionStatus.STOPPING, DnsProtectionStatus.ERROR),
            DnsProtectionStatus.DEGRADED to setOf(DnsProtectionStatus.ACTIVE_DNS_ONLY, DnsProtectionStatus.STOPPING, DnsProtectionStatus.ERROR),
            DnsProtectionStatus.STOPPING to setOf(DnsProtectionStatus.OFF, DnsProtectionStatus.ERROR),
            DnsProtectionStatus.ERROR to setOf(DnsProtectionStatus.OFF, DnsProtectionStatus.REQUESTING_PERMISSION, DnsProtectionStatus.STARTING)
        )
    }
}

object DnsProtectionStateStore {
    private val machine = DnsProtectionStateMachine()
    private val mutableState = MutableStateFlow(machine.state)
    val state: StateFlow<DnsProtectionState> = mutableState.asStateFlow()

    fun current(): DnsProtectionState = mutableState.value

    @Synchronized
    fun transition(
        next: DnsProtectionStatus,
        reason: DnsDegradedReason = DnsDegradedReason.NONE,
        detail: String = ""
    ): Boolean {
        val changed = machine.transition(next, reason, detail)
        if (changed) mutableState.value = machine.state
        return changed
    }
}

fun DnsProtectionState.displayLabel(): String = when (status) {
    DnsProtectionStatus.ACTIVE_DNS_ONLY -> "Protección DNS activa"
    DnsProtectionStatus.DEGRADED -> "Protección DNS degradada: ${reason.label()}"
    DnsProtectionStatus.REQUESTING_PERMISSION -> "Permiso VPN pendiente"
    DnsProtectionStatus.STARTING -> "Iniciando protección DNS"
    DnsProtectionStatus.STOPPING -> "Deteniendo protección DNS"
    DnsProtectionStatus.ERROR -> "Error: ${detail.ifBlank { reason.label() }}"
    DnsProtectionStatus.OFF -> "Protección DNS desactivada"
}

private fun DnsDegradedReason.label(): String = when (this) {
    DnsDegradedReason.NONE -> "sin detalle"
    DnsDegradedReason.FEED_INVALID -> "feed no válido"
    DnsDegradedReason.UPSTREAM_UNAVAILABLE -> "resolución upstream no disponible"
    DnsDegradedReason.DNS_PARSE_FAILURE -> "consulta DNS no interpretable"
    DnsDegradedReason.NETWORK_UNAVAILABLE -> "red no disponible"
    DnsDegradedReason.VPN_REVOKED -> "VPN revocada"
    DnsDegradedReason.INTERNAL_FAILURE -> "fallo interno"
    DnsDegradedReason.BATTERY_RESTRICTED -> "batería restringida"
}