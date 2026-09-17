package com.aura.defense.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsProtectionStateTest {
    @Test
    fun `recorre la ruta activa DNS valida`() {
        val machine = DnsProtectionStateMachine()

        assertTrue(machine.transition(DnsProtectionStatus.REQUESTING_PERMISSION))
        assertTrue(machine.transition(DnsProtectionStatus.STARTING))
        assertTrue(machine.transition(DnsProtectionStatus.ACTIVE_DNS_ONLY))
        assertTrue(machine.transition(DnsProtectionStatus.DEGRADED, DnsDegradedReason.UPSTREAM_UNAVAILABLE))
        assertTrue(machine.transition(DnsProtectionStatus.ACTIVE_DNS_ONLY))
        assertTrue(machine.transition(DnsProtectionStatus.STOPPING))
        assertTrue(machine.transition(DnsProtectionStatus.OFF))
    }

    @Test
    fun `rechaza saltos de estado no validos`() {
        val machine = DnsProtectionStateMachine()

        assertFalse(machine.transition(DnsProtectionStatus.ACTIVE_DNS_ONLY))
        assertFalse(machine.transition(DnsProtectionStatus.ERROR))
        assertTrue(machine.transition(DnsProtectionStatus.REQUESTING_PERMISSION))
        assertFalse(machine.transition(DnsProtectionStatus.ACTIVE_DNS_ONLY))
    }

    @Test
    fun `error permite recuperacion controlada`() {
        val machine = DnsProtectionStateMachine()

        assertTrue(machine.transition(DnsProtectionStatus.STARTING))
        assertTrue(machine.transition(DnsProtectionStatus.ERROR, DnsDegradedReason.INTERNAL_FAILURE))
        assertTrue(machine.transition(DnsProtectionStatus.STARTING))
    }
}