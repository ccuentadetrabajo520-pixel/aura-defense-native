package com.aura.defense.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class DnsDecisionEngineTest {
    private val rules = listOf(
        DnsRule("evil.example", "MALWARE", "bundled", "2026-09-17", "HIGH"),
        DnsRule("tracking.example", "TRACKING", "feed", "v4", "MEDIUM")
    )

    @Test
    fun `decide allow block unknown y error`() {
        val engine = DnsDecisionEngine(
            DnsFirewallProfile.EQUILIBRADO,
            allowlist = setOf("allowed.example"),
            blocklist = setOf("manual.example"),
            rules = rules
        )

        assertEquals(DnsDecision.ALLOW, engine.decide("allowed.example").decision)
        assertEquals(DnsDecision.BLOCK, engine.decide("login.evil.example").decision)
        assertEquals(DnsDecision.BLOCK, engine.decide("manual.example").decision)
        assertEquals(DnsDecision.UNKNOWN, engine.decide("unknown.example").decision)
        assertEquals(DnsDecision.ERROR, engine.decide("bad domain").decision)
    }

    @Test
    fun `perfil permite categoria conocida fuera de su cobertura`() {
        val engine = DnsDecisionEngine(
            DnsFirewallProfile.PERMITIR_TODO,
            emptySet(),
            emptySet(),
            rules
        )

        assertEquals(DnsDecision.ALLOW, engine.decide("tracking.example").decision)
        assertEquals("category_not_in_profile", engine.decide("tracking.example").reason)
    }

    @Test
    fun `excepcion temporal activa y caducada`() {
        val now = 1_000L
        val engine = DnsDecisionEngine(
            DnsFirewallProfile.ESTRICTO,
            emptySet(),
            emptySet(),
            rules,
            temporaryExceptions = listOf(
                DnsTemporaryException("evil.example", 2_000L, "revisión del usuario"),
                DnsTemporaryException("tracking.example", 900L, "caducada")
            ),
            now = { now }
        )

        assertEquals(DnsDecision.ALLOW, engine.decide("evil.example").decision)
        assertEquals(DnsDecision.BLOCK, engine.decide("tracking.example").decision)
    }

    @Test
    fun `feed vacio no bloquea desconocidos y regla invalida da error`() {
        val engine = DnsDecisionEngine(DnsFirewallProfile.EQUILIBRADO, emptySet(), emptySet(), emptyList())

        assertEquals(DnsDecision.UNKNOWN, engine.decide("not-listed.example").decision)
        assertEquals(DnsDecision.ERROR, engine.decide("not a domain").decision)
    }

    @Test
    fun `feed caducado y regla invalida producen error sin bloquear`() {
        val expired = DnsDecisionEngine(
            DnsFirewallProfile.ESTRICTO,
            emptySet(),
            emptySet(),
            listOf(DnsRule("old.example", "MALWARE", "feed", "v1", validUntil = 900L)),
            now = { 1_000L }
        )
        val invalid = DnsDecisionEngine(
            DnsFirewallProfile.ESTRICTO,
            emptySet(),
            emptySet(),
            listOf(DnsRule("bad domain", "MALWARE", "feed", "v1"))
        )

        assertEquals(DnsDecision.ERROR, expired.decide("old.example").decision)
        assertEquals("expired_feed", expired.decide("old.example").reason)
        assertEquals(DnsDecision.ERROR, invalid.decide("safe.example").decision)
        assertEquals("invalid_rule", invalid.decide("safe.example").reason)
    }
}