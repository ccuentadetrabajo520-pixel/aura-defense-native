package com.aura.defense.security

data class CryptoScamResult(
        val input: String,
            val isScam: Boolean,
                val riskLevel: String,
                    val indicators: List<String>,
                        val explanation: String
)

class CryptoScamDetector {

        companion object {
                    private val SCAM_DOMAINS = setOf(
                                    "elon-musk-coin", "free-eth", "crypto-giveaway", "double-your-crypto",
                                                "crypto-airdrop", "elon-gift", "musk-crypto", "free-bitcoin",
                                                            "crypto-reward", "claim-token", "defi-yield-100", "guaranteed-crypto",
                                                                        "crypto-mining-cloud", "instant-profit-crypto", "crypto-multiply",
                                                                                    "binance-giveaway", "coinbase-reward", "metamask-airdrop"
                    )
                            private val SCAM_PHRASES = listOf(
                                            "double your", "100x return", "guaranteed profit", "risk free crypto",
                                                        "send 1 receive 2", "send eth get", "send btc get", "limited time airdrop",
                                                                    "elon musk giveaway", "crypto giveaway", "free nft mint",
                                                                                "no risk investment", "instant withdrawal", "unlimited crypto",
                                                                                            "guaranteed airdrop", "claim free token"
                            )
                                    private val URGENCY_WORDS = listOf("urgent", "hurry", "limited time", "act now", "only today", "ending soon")
                                            private val IMPERSONATION_WORDS = listOf("official", "verified", "support team", "admin")
                                                    private val WALLET_REGEX = Regex("\\b(0x[a-fA-F0-9]{40})\\b")
                                                            private val URL_REGEX = Regex("(https?://[^\\s]+)")
                                                                    private val FAKE_EXCHANGE = listOf("login-", "account-verify", "secure-wallet", "wallet-connect-auth", "update-wallet")
        }

            fun analyze(input: String): CryptoScamResult {
                        val lower = input.lowercase()
                                val indicators = mutableListOf<String>()

                                        SCAM_DOMAINS.forEach { domain ->
                                                    if (domain in lower) indicators.add("Dominio de estafa conocido: $domain")
                                                            }
                                                                    SCAM_PHRASES.forEach { phrase ->
                                                                                if (phrase in lower) indicators.add("Frase sospechosa: \"$phrase\"")
                                                                                        }
                                                                                                val wallets = WALLET_REGEX.findAll(input).map { it.value }.toList()
                                                                                                        if (wallets.isNotEmpty()) indicators.add("${wallets.size} wallet(s) detectada(s). Nunca envies fondos a direcciones desconocidas.")

                                                                                                                URL_REGEX.findAll(input).map { it.value }.toList().forEach { url ->
                                                                                                                            FAKE_EXCHANGE.forEach { pattern ->
                                                                                                                                            if (pattern in url.lower()) indicators.add("URL que imita un exchange: $url")
                                                                                                                                                        }
                                                                                                                                                                }
                                                                                                                                                                        if (URGENCY_WORDS.any { it in lower }) indicators.add("Indicadores de urgencia. Las estafas cripto suelen presionar.")
                                                                                                                                                                                if (IMPERSONATION_WORDS.any { it in lower } && wallets.isNotEmpty()) indicators.add("Posible suplantacion: lenguaje oficial + solicitud de fondos.")

                                                                                                                                                                                        val riskLevel = when {
                                                                                                                                                                                                        indicators.size >= 4 -> "CRITICO"
                                                                                                                                                                                                                    indicators.size >= 2 -> "ALTO"
                                                                                                                                                                                                                                indicators.size >= 1 -> "MEDIO"
                                                                                                                                                                                                                                            else -> "BAJO"
                                                                                                                                                                                        }
                                                                                                                                                                                                val isScam = indicators.size >= 2
                                                                                                                                                                                                        val explanation = when {
                                                                                                                                                                                                                        isScam -> "Multiples indicadores de estafa cripto. NO envies fondos ni conectes tu wallet."
                                                                                                                                                                                                                                    indicators.size == 1 -> "Un indicador sospechoso. Verifica la fuente antes de actuar."
                                                                                                                                                                                                                                                else -> "No se detectaron patrones de estafa cripto."
                                                                                                                                                                                                        }
                                                                                                                                                                                                                return CryptoScamResult(input, isScam, riskLevel, indicators, explanation)
            }
}
