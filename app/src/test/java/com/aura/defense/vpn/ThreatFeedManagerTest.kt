package com.aura.defense.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThreatFeedManagerTest {
    @Test
    fun `coincide dominio exacto o subdominio`() {
        val map = mapOf("evil.com" to "PHISHING")

        assertEquals("PHISHING", ThreatFeedManager.matchDomain(map, "login.evil.com"))
        assertNull(ThreatFeedManager.matchDomain(map, "notevil.com"))
        assertNull(ThreatFeedManager.matchDomain(emptyMap(), "evil.com"))
    }
}
