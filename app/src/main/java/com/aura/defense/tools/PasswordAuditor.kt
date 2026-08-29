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
        if (isCommonPassword(password)) {
            score = 0
            recommendations += "Esta contraseña está en la lista de las 100 contraseñas más comunes. Extremadamente vulnerable."
        }
        val entropy = password.toSet().size * log2((password.length + 1).toDouble())
        score = (score + entropy.toInt() / 4).coerceIn(0, 100)
        val strength = when { score >= 85 -> PasswordStrength.EXCELENTE; score >= 65 -> PasswordStrength.FUERTE; score >= 40 -> PasswordStrength.MEDIA; else -> PasswordStrength.DEBIL }
        return PasswordAudit(strength, score, recommendations.ifEmpty { listOf("No se observan mejoras locales prioritarias") })
    }

    private fun hasSequence(value: String): Boolean {
        val lower = value.lowercase()
        return listOf("1234", "abcd", "qwerty", "password").any { lower.contains(it) }
    }

    private val commonPasswords = setOf(
        "123456", "password", "12345678", "qwerty", "123456789", "12345", "1234", "111111", "1234567", "dragon", "123123", "baseball", "abc123", "football", "monkey", "letmein", "shadow", "master", "666666", "qwertyuiop", "123321", "mustang", "1234567890", "michael", "654321", "superman", "1qaz2wsx", "7777777", "121212", "000000", "qazwsx", "123qwe", "killer", "trustno1", "jordan", "jennifer", "zxcvbnm", "asdfgh", "hunter", "buster", "soccer", "harley", "batman", "andrew", "tigger", "sunshine", "iloveyou", "2000", "charlie", "robert", "thomas", "hockey", "ranger", "daniel", "starwars", "klaster", "112233", "george", "computer", "michelle", "jessica", "pepper", "1111", "zxcvbn", "555555", "131313", "freedom", "777777", "pass", "maggie", "159753", "aaaaaa", "ginger", "princess", "joshua", "cheese", "amanda", "summer", "love", "ashley", "nicole", "chelsea", "biteme", "matthew", "access", "yankees", "987654321", "dallas", "austin", "thunder", "taylor", "matrix", "mobilemail", "mom", "monitor", "monitoring", "montana", "moon", "moscow"
    )

    private fun isCommonPassword(password: String): Boolean = commonPasswords.contains(password.lowercase())
}
