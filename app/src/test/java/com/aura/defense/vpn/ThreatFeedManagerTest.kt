package com.aura.defense.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThreatFeedManagerTest {
    @Test
    fun `threat feed manager is disabled as active security source`() {
        val map = mapOf("evil.com" to "PHISHING")

        assertNull(ThreatFeedManager.matchDomain(map, "login.evil.com"))
        assertNull(ThreatFeedManager.matchDomain(map, "notevil.com"))
        assertNull(ThreatFeedManager.matchDomain(emptyMap(), "evil.com"))
        assertEquals(0, ThreatFeedManager.size())
    }
}
