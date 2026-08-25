package com.aura.defense.threats

import android.content.Context
import java.net.URI

enum class ThreatIndicatorType { DOMAIN, URL_PATTERN, KEYWORD }
enum class ThreatCategory { PHISHING, MALWARE, SPYWARE, BOTNET, C2, TRACKING, ADS, CRYPTO_SCAM }
enum class ThreatSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class ThreatIndicator(
    val id: String,
    val indicator: String,
    val indicatorType: ThreatIndicatorType,
    val category: ThreatCategory,
    val severity: ThreatSeverity,
    val descriptionEs: String,
    val source: String,
    val updatedAt: String
) {
    companion object {
        fun fromJson(value: org.json.JSONObject) = ThreatIndicator(
            id = value.getString("id"),
            indicator = value.getString("indicator").trim(),
            indicatorType = ThreatIndicatorType.valueOf(value.getString("indicatorType")),
            category = ThreatCategory.valueOf(value.getString("category")),
            severity = ThreatSeverity.valueOf(value.getString("severity")),
            descriptionEs = value.getString("descriptionEs"),
            source = value.getString("source"),
            updatedAt = value.getString("updatedAt")
        )
    }
}

class ThreatIntelligenceEngine(context: Context) {
    val indicators: List<ThreatIndicator> = ThreatIntelligenceRepository(context).load()
    val lastUpdatedAt: String = indicators.maxOfOrNull { it.updatedAt } ?: "No disponible"
    val categories: List<ThreatCategory> = indicators.map { it.category }.distinct()

    fun findMatches(input: String): List<ThreatIndicator> {
        val value = input.trim()
        if (value.isBlank()) return emptyList()
        val normalizedUrl = normalizeUrl(value)
        val normalizedDomain = extractDomain(normalizedUrl)
        return indicators.filter { indicator ->
            when (indicator.indicatorType) {
                ThreatIndicatorType.DOMAIN -> normalizedDomain == indicator.indicator.lowercase() || normalizedDomain.endsWith(".${indicator.indicator.lowercase()}")
                ThreatIndicatorType.URL_PATTERN -> normalizedUrl.contains(indicator.indicator.lowercase())
                ThreatIndicatorType.KEYWORD -> normalizedUrl.contains(indicator.indicator.lowercase())
            }
        }
    }

    private fun normalizeUrl(value: String): String = value.lowercase().trim().let { if (it.startsWith("www.")) "https://$it" else it }

    private fun extractDomain(value: String): String = runCatching {
        URI(if (value.contains("://")) value else "https://$value").host.orEmpty().lowercase().trimEnd('.')
    }.getOrDefault("")
}
