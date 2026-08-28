package com.aura.defense.security

import com.aura.defense.vpn.ThreatFeedManager
import com.aura.defense.vpn.VpnDebugger
import java.net.URL
import java.net.URLDecoder

data class ThreatAnalysis(
    val score: Int,
    val level: String,
    val reason: String
)

object SocialEngineDetector {
    private val manipulationKeywords = listOf(
        "urgent", "urgente", "verify your account", "account suspended", "suspendida", "blocked",
        "negative balance", "prize", "win", "click here", "download now",
        "secure", "mandatory update", "confidential information", "fraud",
        "recover access", "last chance", "expires", "verification required",
        "action required", "suspended", "mitigation"
    )

    private val suspiciousTLDs = listOf(".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top", ".work")

    fun analyze(inputText: String): ThreatAnalysis {
        var score = 0
        val reasons = mutableListOf<String>()
        val text = inputText.lowercase()

        try {
            val urlStr = text.split("\\s+".toRegex()).firstOrNull() ?: text
            if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
                val url = URL(urlStr)
                if (suspiciousTLDs.any { url.host.endsWith(it) }) {
                    score += 25
                    reasons.add("High-risk domain (.tk, .ml, etc.)")
                }
                if (urlStr.contains("%")) {
                    try {
                        val decoded = URLDecoder.decode(urlStr, "UTF-8")
                        if (decoded != urlStr) {
                            score += 30
                            reasons.add("URL intentionally obfuscated to bypass filters")
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }

        var matchCount = 0
        for (keyword in manipulationKeywords) {
            if (text.contains(keyword)) {
                VpnDebugger.log("🎯 Manipulación detectada: $keyword")
                matchCount++
                if (matchCount <= 2) reasons.add("Contains urgency/manipulation language: '$keyword'")
            }
        }
        if (matchCount == 1) score += 15
        if (matchCount == 2) score += 30
        if (matchCount >= 3) score += 50

        try {
            val urlStr = text.split("\\s+".toRegex()).firstOrNull()
            if (urlStr != null && ThreatFeedManager.isBlocked(urlStr)) {
                score += 100
                reasons.add("Domain confirmed in global Phishing/Malware blocklists")
            }
        } catch (_: Exception) {
        }

        val level = when {
            score >= 80 -> "Critical"
            score >= 50 -> "High"
            score >= 20 -> "Medium"
            else -> "Low"
        }
        return ThreatAnalysis(score.coerceIn(0, 100), level, if (reasons.isEmpty()) "Clean analysis" else reasons.joinToString(". "))
    }
}