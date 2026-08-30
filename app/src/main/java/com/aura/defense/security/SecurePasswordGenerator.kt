package com.aura.defense.security

import kotlin.math.log2
import kotlin.math.pow
import kotlin.random.Random

data class GeneratedPassword(
        val password: String,
            val length: Int,
                val hasUppercase: Boolean,
                    val hasLowercase: Boolean,
                        val hasDigits: Boolean,
                            val hasSymbols: Boolean,
                                val entropyBits: Double
)

enum class StrengthLevel { VERY_WEAK, WEAK, MODERATE, STRONG, VERY_STRONG }

data class PasswordStrength(val entropy: Double, val crackTime: String, val level: StrengthLevel)

class SecurePasswordGenerator {

        companion object {
                    private val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                            private val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
                                    private val DIGITS = "0123456789"
                                            private val SYMBOLS = "!@#\$%^&*()-_=+[]{}|;:,.<>?"
                                                    private val AMBIGUOUS = setOf('l', 'I', '1', 'O', '0')
        }

            fun generate(
                        length: Int = 16,
                                includeUppercase: Boolean = true,
                                        includeLowercase: Boolean = true,
                                                includeDigits: Boolean = true,
                                                        includeSymbols: Boolean = true,
                                                                excludeAmbiguous: Boolean = false
            ): GeneratedPassword {
                        var charset = ""
                                var guaranteed = ""
                                        if (includeUppercase) { charset += UPPERCASE; guaranteed += randomFrom(UPPERCASE) }
                                                if (includeLowercase) { charset += LOWERCASE; guaranteed += randomFrom(LOWERCASE) }
                                                        if (includeDigits) { charset += DIGITS; guaranteed += randomFrom(DIGITS) }
                                                                if (includeSymbols) { charset += SYMBOLS; guaranteed += randomFrom(SYMBOLS) }

                                                                        var effectiveCharset = charset
                                                                                if (excludeAmbiguous) effectiveCharset = charset.filter { it !in AMBIGUOUS }

                                                                                        val finalLength = length.coerceIn(8, 64)
                                                                                                val sb = StringBuilder(guaranteed)
                                                                                                        while (sb.length < finalLength) {
                                                                                                                        sb.append(randomFrom(effectiveCharset))
                                                                                                        }
                                                                                                                val password = sb.toString().toCharArray().also { it.shuffle() }.concatToString()

                                                                                                                        val poolSize = effectiveCharset.length.toDouble().coerceAtLeast(1.0)
                                                                                                                                val entropy = finalLength * kotlin.math.log2(poolSize)

                                                                                                                                        return GeneratedPassword(password, password.length, includeUppercase, includeLowercase, includeDigits, includeSymbols, entropy)
            }

                fun estimateStrength(password: String): PasswordStrength {
                            val length = password.length
                                    val hasUpper = password.any { it.isUpperCase() }
                                            val hasLower = password.any { it.isLowerCase() }
                                                    val hasDigit = password.any { it.isDigit() }
                                                            val hasSymbol = password.any { !it.isLetterOrDigit() }
                                                                    val poolSize = (if (hasUpper) 26 else 0) + (if (hasLower) 26 else 0) + (if (hasDigit) 10 else 0) + (if (hasSymbol) 32 else 0)
                                                                            val entropy = if (poolSize > 0) length * kotlin.math.log2(poolSize.toDouble()) else 0.0

                                                                                    val crackSeconds = pow(2.0, entropy) / 1e10
                                                                                            val crackTime = when {
                                                                                                            crackSeconds < 1 -> "instantaneamente"
                                                                                                                        crackSeconds < 60 -> "${crackSeconds.toLong()} segundos"
                                                                                                                                    crackSeconds < 3600 -> "${(crackSeconds / 60).toLong()} minutos"
                                                                                                                                                crackSeconds < 86400 -> "${(crackSeconds / 3600).toLong()} horas"
                                                                                                                                                            crackSeconds < 31536000 -> "${(crackSeconds / 86400).toLong()} dias"
                                                                                                                                                                        crackSeconds < 3.15e10 -> "${(crackSeconds / 31536000).toLong()} anos"
                                                                                                                                                                                    crackSeconds < 3.15e13 -> "${(crackSeconds / 3.15e10).toLong()} mil anos"
                                                                                                                                                                                                else -> "miles de millones de anos"
                                                                                            }
                                                                                                    val level = when {
                                                                                                                    entropy >= 100 -> StrengthLevel.VERY_STRONG
                                                                                                                                entropy >= 80 -> StrengthLevel.STRONG
                                                                                                                                            entropy >= 60 -> StrengthLevel.MODERATE
                                                                                                                                                        entropy >= 40 -> StrengthLevel.WEAK
                                                                                                                                                                    else -> StrengthLevel.VERY_WEAK
                                                                                                    }
                                                                                                            return PasswordStrength(entropy, crackTime, level)
                }

                    private fun randomFrom(chars: String): Char {
                                val filtered = chars.ifEmpty { LOWERCASE }
                                        return filtered[Random.nextInt(filtered.length)]
                    }
}
