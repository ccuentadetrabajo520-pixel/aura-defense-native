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

        val advancedFindings = detectAdvancedPatterns(inputText)
        advancedFindings.forEach { finding ->
            val weight = when (finding.severity) {
                FindingSeverity.LOW -> 10
                FindingSeverity.MEDIUM -> 25
                FindingSeverity.HIGH -> 40
                FindingSeverity.CRITICAL -> 65
            }
            score += weight
            reasons.add(finding.explanation)
        }

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

    private fun detectAdvancedPatterns(text: String): List<SecurityFinding> {
        val lower = text.lowercase()
        val findings = mutableListOf<SecurityFinding>()

        val urgencyWords = listOf(
            "urgente", "inmediato", "ahora", "ya", "hoy", "última oportunidad",
            "se acaba el tiempo", "actúa ahora", "no esperes", "antes de que sea tarde"
        )
        if (urgencyWords.any { lower.contains(it) }) {
            findings.add(
                SecurityFinding(
                    title = "Urgencia manipuladora",
                    severity = FindingSeverity.MEDIUM,
                    evidence = "Palabras de urgencia detectadas: ${urgencyWords.filter { lower.contains(it) }.joinToString()}",
                    explanation = "Se detectó lenguaje de urgencia manipuladora. Táctica común en ingeniería social para impedir que la víctima piense críticamente.",
                    recommendedAction = "Tómate tiempo para validar la solicitud antes de actuar."
                )
            )
        }

        val authorityWords = listOf(
            "soporte técnico", "técnico de", "admin", "administrador",
            "departamento de seguridad", "equipo de seguridad", "banco",
            "bancomer", "bbva", "santander", "paypal", "amazon", "google",
            "microsoft", "apple soporte"
        )
        val personalDataWords = listOf("contraseña", "password", "pin", "tarjeta", "cvv")
        if (authorityWords.any { lower.contains(it) } && personalDataWords.any { lower.contains(it) }) {
            findings.add(
                SecurityFinding(
                    title = "Suplantación de autoridad",
                    severity = FindingSeverity.HIGH,
                    evidence = "Se detectan indicios de autoridad institucional y petición de datos sensibles.",
                    explanation = "Posible suplantación de autoridad. El mensaje se hace pasar por una entidad oficial y solicita datos sensibles.",
                    recommendedAction = "Verifica el contacto por canales oficiales antes de compartir información personal o financiera."
                )
            )
        }

        val sextortionWords = listOf(
            "video íntimo", "fotos privadas", "contactos", "voy a enviar",
            "voy a publicar", "tus fotos", "grabación", "webcam", "solo pagaré",
            "transferencia", "bitcoin", "criptomoneda"
        )
        val sextortionMatches = sextortionWords.filter { lower.contains(it) }
        if (sextortionMatches.size >= 3) {
            findings.add(
                SecurityFinding(
                    title = "Riesgo de sextorsión",
                    severity = FindingSeverity.CRITICAL,
                    evidence = "Coincidencias: ${sextortionMatches.joinToString()}",
                    explanation = "Patrón de sextorsión detectado. No envíes dinero ni respondas. Reporta a las autoridades.",
                    recommendedAction = "Bloquea y reporta el mensaje; no interactúes ni pagues."
                )
            )
        }

        val techSupportWords = listOf(
            "tu equipo está infectado", "virus detectado", "llama al", "soporte al cliente",
            "número de teléfono", "descarga este programa", "instala este antivirus",
            "tu computadora está en riesgo", "elimina los virus", "paga por la limpieza"
        )
        if (techSupportWords.any { lower.contains(it) }) {
            findings.add(
                SecurityFinding(
                    title = "Estafa de soporte técnico",
                    severity = FindingSeverity.HIGH,
                    evidence = "Se detectó lenguaje típico de estafa técnica y presión para instalar software o llamar a números.",
                    explanation = "Posible estafa de soporte técnico. Los legítimos soportes técnicos nunca llaman ni piden instalar software remoto.",
                    recommendedAction = "No llames ni instales software sugerido por el mensaje; consulta a soporte oficial."
                )
            )
        }

        return findings
    }
}