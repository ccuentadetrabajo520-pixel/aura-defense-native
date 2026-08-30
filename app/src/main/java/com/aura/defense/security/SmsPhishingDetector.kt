package com.aura.defense.security

data class SmsPhishingResult(
        val smsText: String,
            val isPhishing: Boolean,
                val riskLevel: String,
                    val indicators: List<String>,
                        val explanation: String
)

class SmsPhishingDetector {

        companion object {
                    private val SUSPICIOUS_TLDS = listOf("xyz", "top", "click", "win", "bid", "stream", "download", "rng", "loan", "work", "party", "review", "trade", "date", "men", "cricket")
                            private val URL_SHORTENERS = listOf("bit.ly", "tinyurl.com", "t.co", "ow.ly", "is.gd", "rb.gy", "cutt.ly")
                                    private val URGENCY_PHRASES = listOf(
                                                    "tu cuenta sera", "tu cuenta ha sido", "bloqueada", "suspended",
                                                                "verifica tu identidad", "confirma tu cuenta", "accion requerida",
                                                                            "urgente", "inmediatamente", "en 24 horas", "sera cerrada",
                                                                                        "actua ahora", "riesgo de perder", "sin acceso"
                                    )
                                            private val BRANDS = listOf(
                                                            "banco", "paypal", "amazon", "google", "apple", "microsoft",
                                                                        "netflix", "facebook", "whatsapp", "gmail", "outlook",
                                                                                    "santander", "bbva", "banamex", "bancolombia", "nubank",
                                                                                                "chase", "wells fargo", "bank of america", "zelle", "venmo"
                                            )
                                                    private val SENSITIVE_REQUESTS = listOf(
                                                                    "contrasena", "password", "clave", "pin", "numero de tarjeta",
                                                                                "cvv", "fecha de vencimiento", "codigo de verificacion",
                                                                                            "saldo", "numero de cuenta", "datos bancarios"
                                                    )
                                                            private val URL_REGEX = Regex("(https?://[^\\s]+)")
                                                                    private val IP_URL_REGEX = Regex("https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")
        }

            fun analyze(smsText: String): SmsPhishingResult {
                        val lower = smsText.lowercase()
                                val indicators = mutableListOf<String>()
                                        val urls = URL_REGEX.findAll(smsText).map { it.value }.toList()

                                                urls.forEach { url ->
                                                            val urlLower = url.lowercase()
                                                                        val tld = urlLower.removePrefix("https://").removePrefix("http://")
                                                                                        .substringAfterLast(".").split("/").first()
                                                                                                    if (tld in SUSPICIOUS_TLDS) {
                                                                                                                        indicators.add("URL con TLD sospechoso (.$tld): $url")
                                                                                                    }
                                                                                                                if (IP_URL_REGEX.containsMatchIn(url)) {
                                                                                                                                    indicators.add("URL con IP directa: $url")
                                                                                                                }
                                                                                                                            URL_SHORTENERS.forEach { shortener ->
                                                                                                                                            if (shortener in urlLower) {
                                                                                                                                                                    indicators.add("Acortador de URL ($shortener): $url")
                                                                                                                                            }
                                                                                                                            }
                                                                                                                                        BRANDS.forEach { brand ->
                                                                                                                                                        if (brand in urlLower && tld in SUSPICIOUS_TLDS) {
                                                                                                                                                                                indicators.add("URL que suplanta a $brand: $url")
                                                                                                                                                        }
                                                                                                                                        }
                                                }

                                                        URGENCY_PHRASES.forEach { phrase ->
                                                                    if (phrase in lower) {
                                                                                        indicators.add("Urgencia: \"$phrase\"")
                                                                    }
                                                        }

                                                                var impersonatedBrand: String? = null
                                                                        BRANDS.forEach { brand ->
                                                                                    if (brand in lower) {
                                                                                                        impersonatedBrand = brand
                                                                                                                        indicators.add("Posible suplantacion de: $brand")
                                                                                    }
                                                                        }

                                                                                SENSITIVE_REQUESTS.forEach { info ->
                                                                                            if (info in lower) {
                                                                                                                indicators.add("Solicita datos sensibles: \"$info\"")
                                                                                            }
                                                                                }

                                                                                        if (urls.isNotEmpty() && impersonatedBrand != null) {
                                                                                                        indicators.add("CRITICO: URL + suplantacion de marca")
                                                                                        }
                                                                                                if (urls.isNotEmpty() && SENSITIVE_REQUESTS.any { it in lower }) {
                                                                                                                indicators.add("CRITICO: URL + solicitud de datos sensibles")
                                                                                                }

                                                                                                        val riskLevel = when {
                                                                                                                        indicators.any { it.startsWith("CRITICO") } -> "CRITICO"
                                                                                                                                    indicators.size >= 4 -> "ALTO"
                                                                                                                                                indicators.size >= 2 -> "MEDIO"
                                                                                                                                                            indicators.size == 1 -> "BAJO"
                                                                                                                                                                        else -> "LIMPIO"
                                                                                                        }
                                                                                                                val isPhishing = riskLevel == "CRITICO" || riskLevel == "ALTO"
                                                                                                                        val explanation = when {
                                                                                                                                        isPhishing -> "Multiples indicadores de phishing. No hagas clic en los enlaces ni respondas con informacion personal."
                                                                                                                                                    indicators.size == 2 -> "Algunos indicadores sospechosos. Verifica la fuente antes de actuar."
                                                                                                                                                                indicators.size == 1 -> "Un indicador sospechoso. Verifica la fuente."
                                                                                                                                                                            else -> "No se detectaron patrones de phishing en este SMS."
                                                                                                                        }
                                                                                                                                return SmsPhishingResult(smsText, isPhishing, riskLevel, indicators, explanation)
            }
}
