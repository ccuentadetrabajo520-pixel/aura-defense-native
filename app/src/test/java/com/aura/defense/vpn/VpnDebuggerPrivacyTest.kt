package com.aura.defense.vpn

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test

class VpnDebuggerPrivacyTest {
    @Test
    fun `redacta dominios urls e ips de los logs`() = runBlocking {
        VpnDebugger.logs.value = emptyList()
        VpnDebugger.log("blocked https://evil.example/path from 192.0.2.10")

        val entry = VpnDebugger.logs.first().single()
        assertFalse(entry.contains("evil.example"))
        assertFalse(entry.contains("192.0.2.10"))
        assertFalse(entry.contains("https://"))
    }
}