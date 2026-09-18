package com.aura.defense.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.aura.defense.reports.AuraReportBuilder

class SecurityUnitTest {
    @Test
    fun `genera contrasena con longitud y categorias solicitadas`() {
        val generated = SecurePasswordGenerator().generate(24)

        assertEquals(24, generated.password.length)
        assertTrue(generated.password.any(Char::isUpperCase))
        assertTrue(generated.password.any(Char::isLowerCase))
        assertTrue(generated.password.any(Char::isDigit))
        assertTrue(generated.password.any { !it.isLetterOrDigit() })
    }

    @Test
    fun `calcula TOTP RFC 6238`() {
        val secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

        assertEquals("94287082", generateTotpCode(secret, 59, 8))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza generador sin categorias`() {
        SecurePasswordGenerator().generate(includeUppercase = false, includeLowercase = false, includeDigits = false, includeSymbols = false)
    }

    @Test
    fun `excluye caracteres ambiguos tambien en categorias obligatorias`() {
        val generated = SecurePasswordGenerator().generate(
            length = 64,
            includeUppercase = true,
            includeLowercase = true,
            includeDigits = true,
            includeSymbols = false,
            excludeAmbiguous = true
        )

        assertTrue(generated.password.none { it in setOf('l', 'I', '1', 'O', '0') })
    }

    @Test
    fun `el informe JSON conserva caracteres especiales`() {
        val json = AuraReportBuilder().json(
            auraId = "aura\"id\\1",
            posture = PostureResult.pending(),
            apps = null,
            links = emptyList(),
            password = null
        )

        assertTrue(json.contains("\"auraId\""))
        assertTrue(json.contains("aura\\\"id\\\\1"))
        assertTrue(json.contains("aura\"id\\1") || json.contains("aura\\\"id\\\\1"))
    }
}