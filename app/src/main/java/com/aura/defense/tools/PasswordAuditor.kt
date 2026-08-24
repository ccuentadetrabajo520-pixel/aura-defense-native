package com.aura.defense.tools

import kotlin.math.log2

enum class PasswordStrength { DEBIL, MEDIA, FUERTE, EXCELENTE }

data class PasswordAudit(val strength: PasswordStrength, val score: Int, val recommendations: List<String>)

class PasswordAuditor {
    fun audit(password: String): PasswordAudit {
        if (password.isEmpty()) return PasswordAudit(PasswordStrength.DEBIL, 0, listOf("Introduce una contraseña para revisarla", "La contraseña nunca se guarda ni se registra"))
        var score = 0
        val recommendations = mutableListOf<String>()
        if (password.length >= 8) score += 20 else recommendations += "Usa al menos 8 caracteres"
        if (password.length >= 12) score += 20 else recommendations += "Considera usar 12 caracteres o más"
        if (password.any(Char::isUpperCase)) score += 10 else recommendations += "Añade letras mayúsculas"
        if (password.any(Char::isLowerCase)) score += 10 else recommendations += "Añade letras minúsculas"
        if (password.any(Char::isDigit)) score += 10 else recommendations += "Añade números"
        if (password.any { !it.isLetterOrDigit() }) score += 15 else recommendations += "Añade símbolos"
        if (password.windowed(2).any { it[0] == it[1] }) { score -= 10; recommendations += "Evita caracteres repetidos" }
        if (hasSequence(password)) { score -= 20; recommendations += "Evita secuencias predecibles" }
        if (COMMON_PASSWORDS.contains(password.lowercase())) { score = 0; recommendations += "Evita contraseñas habituales" }
        val entropy = password.toSet().size * log2((password.length + 1).toDouble())
        score = (score + entropy.toInt() / 4).coerceIn(0, 100)
        val strength = when { score >= 85 -> PasswordStrength.EXCELENTE; score >= 65 -> PasswordStrength.FUERTE; score >= 40 -> PasswordStrength.MEDIA; else -> PasswordStrength.DEBIL }
        return PasswordAudit(strength, score, recommendations.ifEmpty { listOf("No se observan mejoras locales prioritarias") })
    }

    private fun hasSequence(value: String): Boolean {
        val lower = value.lowercase()
        return listOf("1234", "abcd", "qwerty", "password").any { lower.contains(it) }
    }

    private companion object { val COMMON_PASSWORDS = setOf("123456", "password", "contraseña", "qwerty", "admin", "12345678") }
}
