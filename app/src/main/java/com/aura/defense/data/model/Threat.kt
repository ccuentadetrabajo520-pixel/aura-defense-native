package com.aura.defense.data.model

data class Threat(
    val id: String,
    val name: String,
    val severity: ThreatSeverity,
    val description: String,
    val detectedAt: String,
    val packageName: String = "",
    val reasons: List<String> = emptyList()
)

enum class ThreatSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}