package com.aura.defense.security

import com.aura.defense.data.DeviceTelemetrySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SecurityPostureEngineTest {
    private val engine = SecurityPostureEngine()

    @Test
    fun `telemetria pendiente conserva score pendiente`() {
        assertEquals(-1, engine.evaluate(PostureResult.pending().telemetry).score)
    }

    @Test
    fun `snapshot completo sin hallazgos tiene cobertura activa`() {
        val result = engine.evaluate(snapshot())
        assertEquals(100, result.score)
        assertEquals("COBERTURA_ACTIVA", result.status)
    }

    @Test
    fun `sin vpn activo no se muestra cobertura activa`() {
        val result = engine.evaluate(snapshot(vpnActive = false))
        assertEquals("PROTECCION_DETENIDA", result.status)
        assertNotEquals("Protegido", result.status)
    }

    @Test
    fun `si hay cobertura parcial o evidencia insuficiente no se anuncia protegido`() {
        val partial = engine.evaluate(snapshot(privateDnsStatus = "No disponible"))
        val insufficient = engine.evaluate(snapshot(networkActive = "No disponible"))

        assertEquals("COBERTURA_PARCIAL", partial.status)
        assertEquals("EVIDENCIA_INSUFICIENTE", insufficient.status)
        assertNotEquals("Protegido", partial.status)
        assertNotEquals("Protegido", insufficient.status)
    }

    private fun snapshot(
        apiLevel: Int = 34,
        screenLockSecure: Boolean = true,
        vpnActive: Boolean = true,
        privateDnsStatus: String = "Activo",
        networkActive: String = "Wi-Fi"
    ) = DeviceTelemetrySnapshot(
        manufacturer = "Google",
        model = "Pixel",
        androidVersion = "14",
        apiLevel = apiLevel,
        securityPatch = "2099-01-01",
        batteryLevel = "80%",
        ramAvailableBytes = 1L,
        ramTotalBytes = 2L,
        storageAvailableBytes = 1L,
        storageTotalBytes = 2L,
        networkActive = networkActive,
        vpnActive = vpnActive,
        privateDnsStatus = privateDnsStatus,
        screenLockSecure = screenLockSecure,
        adbEnabled = false,
        accessibilityServices = emptyList()
    )
}
