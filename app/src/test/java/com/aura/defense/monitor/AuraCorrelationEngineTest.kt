package com.aura.defense.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuraCorrelationEngineTest {
    @Test
    fun `admins de proveedores conocidos no generan alertas`() {
        val alerts = AuraCorrelationEngine.detectStalkerwarePattern(
            adminPackages = listOf("com.android.settings", "com.google.android.gms"),
            notificationListeners = listOf("com.other.listener"),
            flaggedAdminPackages = setOf("com.android.settings", "com.google.android.gms")
        )

        assertEquals(0, alerts.size)
    }

    @Test
    fun `admin desconocido y listener externo generan alerta critica`() {
        val alerts = AuraCorrelationEngine.detectStalkerwarePattern(
            adminPackages = listOf("com.spy.app"),
            notificationListeners = listOf("com.other.listener"),
            flaggedAdminPackages = setOf("com.spy.app")
        )

        assertEquals(1, alerts.size)
        assertTrue(alerts.single().severity == "CRITICAL")
    }
}
