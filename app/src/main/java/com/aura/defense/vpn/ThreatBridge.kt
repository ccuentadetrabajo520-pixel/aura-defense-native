package com.aura.defense.vpn

import com.aura.defense.threats.ThreatIntelligenceEngine

object ThreatBridge {
    data class EnrichedResult(
        val blocked: Boolean,
        val source: String?,
        val category: String?,
        val severity: String?
    )

    fun enrichDomainCheck(domain: String, threatEngine: ThreatIntelligenceEngine?): EnrichedResult {
        threatEngine?.findMatches(domain)?.maxByOrNull { it.severity.ordinal }?.let { best ->
            return EnrichedResult(true, "LOCAL_INTEL", best.category.name, best.severity.name)
        }
        ThreatFeedManager.categoryOf(domain)?.let { category ->
            return EnrichedResult(true, "FEED", category, "HIGH")
        }
        return EnrichedResult(false, null, null, null)
    }
}
