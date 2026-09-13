package com.aura.defense.security

import kotlin.math.pow
import java.security.SecureRandom

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

        private val secureRandom = SecureRandom()

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
                                                require(includeUppercase || includeLowercase || includeDigits || includeSymbols) {
                                                        "Debes seleccionar al menos un tipo de carácter"
                                                }
                                                val requestedPools = buildList {
                                                        if (includeUppercase) add(UPPERCASE)
                                                        if (includeLowercase) add(LOWERCASE)
                                                        if (includeDigits) add(DIGITS)
                                                        if (includeSymbols) add(SYMBOLS)
                                                }
                                                val charset = requestedPools.joinToString("")
                                                val effectiveCharset = if (excludeAmbiguous) {
                                                        charset.filter { it !in AMBIGUOUS }
                                                } else {
                                                        charset
                                                }
                                                require(effectiveCharset.isNotEmpty()) {
                                                        "La combinación elegida no deja caracteres disponibles"
                                                }
                                                val guaranteed = requestedPools.joinToString("") { pool ->
                                                        randomFrom(if (excludeAmbiguous) pool.filter { it !in AMBIGUOUS } else pool).toString()
                                                }

                                                                                        val finalLength = length.coerceIn(8, 64)
                                                                                                val sb = StringBuilder(guaranteed)
                                                                                                        while (sb.length < finalLength) {
                                                                                                                        sb.append(randomFrom(effectiveCharset))
                                                                                                        }
                                                                                                                val password = sb.toString().toCharArray().also(::secureShuffle).concatToString()

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

                                                                                    val crackSeconds = 2.0.pow(entropy) / 1e10
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
                                        return filtered[secureRandom.nextInt(filtered.length)]
                    }

                        private fun secureShuffle(chars: CharArray) {
                                    for (index in chars.lastIndex downTo 1) {
                                                val otherIndex = secureRandom.nextInt(index + 1)
                                                        val value = chars[index]
                                                                chars[index] = chars[otherIndex]
                                                                        chars[otherIndex] = value
                                    }
                        }
}
