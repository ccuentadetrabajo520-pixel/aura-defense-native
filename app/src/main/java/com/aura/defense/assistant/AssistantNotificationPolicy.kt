package com.aura.defense.assistant

import com.aura.defense.threats.ThreatRepositoryState
import com.aura.defense.vpn.DnsProtectionStatus

data class AssistantNotificationDecision(val title: String, val text: String)

object AssistantNotificationPolicy {
    fun decide(dnsStatus: DnsProtectionStatus, feedState: ThreatRepositoryState): AssistantNotificationDecision? = when {
        dnsStatus == DnsProtectionStatus.OFF -> AssistantNotificationDecision("Protección DNS detenida", "Cobertura actual: DNS detenido.")
        dnsStatus == DnsProtectionStatus.DEGRADED -> AssistantNotificationDecision("Cobertura degradada", "Cobertura actual: revisa el estado local de AURA.")
        feedState == ThreatRepositoryState.EXPIRED -> AssistantNotificationDecision("Feed de inteligencia caducado", "Cobertura actual: el feed vigente no está disponible.")
        feedState == ThreatRepositoryState.FAILED -> AssistantNotificationDecision("Feed de inteligencia no disponible", "Cobertura actual: no hay feed verificado disponible.")
        else -> null
    }
}
