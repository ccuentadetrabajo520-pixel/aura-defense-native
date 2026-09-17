package com.aura.defense.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionReadinessTest {
    @Test
    fun `solo permite protegido cuando los tres mecanismos estan activos`() {
        assertTrue(ProtectionReadiness.isProtected(true, 1, true))
    }

    @Test
    fun `vpn activa sin feed no es protegido`() {
        assertFalse(ProtectionReadiness.isProtected(true, 0, true))
    }

    @Test
    fun `vpn y feed activos sin motor no es protegido`() {
        assertFalse(ProtectionReadiness.isProtected(true, 1, false))
    }

    @Test
    fun `feed y motor activos sin vpn no es protegido`() {
        assertFalse(ProtectionReadiness.isProtected(false, 1, true))
    }
}