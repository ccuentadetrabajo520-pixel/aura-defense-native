package com.aura.defense.tools

import java.net.InetAddress
import java.net.URI

enum class LinkRisk { SEGURO, SOSPECHOSO, PELIGROSO }

data class LinkAnalysis(val url: String, val risk: LinkRisk, val reasons: List<String>)

class LinkAnalyzer {
    fun analyze(input: String): LinkAnalysis {
        val value = input.trim()
        if (value.isBlank()) return LinkAnalysis(value, LinkRisk.SOSPECHOSO, listOf("Introduce una URL para analizarla"))
        val reasons = buildList {
            val lower = value.lowercase()
            runCatching {
                val uri = URI(value)
                if (uri.scheme.equals("http", true)) add("Usa HTTP sin cifrado")
                val host = uri.host.orEmpty()
                if (host.isBlank()) add("Dominio con formato sospechoso")
                if (host.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}"))) add("Usa una dirección IP directa")
                if (host.contains("xn--")) add("Dominio con formato internacional")
                if (host.count { it == '.' } > 3) add("Demasiados subdominios")
                if (host.endsWithAny("bit.ly", "tinyurl.com", "t.co", "is.gd", "goo.gl")) add("Acortador detectado")
                if (host.substringAfterLast('.', "").length <= 2) add("Extensión de dominio poco habitual")
            }.onFailure { add("Dominio con formato sospechoso") }
            if (value.length > 180) add("URL demasiado larga")
            if (lower.containsAny("login", "verify", "wallet", "bank", "reset", "prize", "crypto", "seed", "password")) add("Palabras sensibles detectadas")
        }
        val risk = when {
            reasons.any { it == "Usa HTTP sin cifrado" && reasons.size >= 3 } || reasons.size >= 4 -> LinkRisk.PELIGROSO
            reasons.isNotEmpty() -> LinkRisk.SOSPECHOSO
            else -> LinkRisk.SEGURO
        }
        return LinkAnalysis(value, risk, reasons.ifEmpty { listOf("No se observaron señales locales de riesgo") })
    }

    private fun String.containsAny(vararg values: String) = values.any { contains(it) }
    private fun String.endsWithAny(vararg values: String) = values.any { endsWith(it) }
}
