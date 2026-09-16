package com.aura.defense.security

import com.aura.defense.data.DeviceTelemetrySnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class SecurityPostureEngineTest {
    private val engine = SecurityPostureEngine()

    @Test
    fun `telemetria pendiente conserva score pendiente`() {
        assertEquals(-1, engine.evaluate(PostureResult.pending().telemetry).score)
    }

    @Test
    fun `snapshot completo sin hallazgos puntua cien`() {
        assertEquals(100, engine.evaluate(snapshot()).score)
    }

    @Test
    fun `un hallazgo medio resta nueve`() {
        assertEquals(91, engine.evaluate(snapshot(apiLevel = 28)).score)
    }

    @Test
    fun `un hallazgo alto resta dieciocho`() {
        assertEquals(82, engine.evaluate(snapshot(screenLockSecure = false)).score)
    }

    private fun snapshot(
        apiLevel: Int = 34,
        screenLockSecure: Boolean = true
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
        networkActive = "Wi-Fi",
        vpnActive = true,
        privateDnsStatus = "Activo",
        screenLockSecure = screenLockSecure,
        adbEnabled = false,
        accessibilityServices = emptyList()
    )
}
