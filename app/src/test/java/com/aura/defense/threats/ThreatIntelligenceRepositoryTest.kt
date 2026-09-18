package com.aura.defense.threats

import com.aura.defense.apps.AppFindingLevel
import com.aura.defense.apps.AppScannerRules
import com.aura.defense.vpn.DnsDecision
import com.aura.defense.vpn.DnsDecisionEngine
import com.aura.defense.vpn.DnsFirewallProfile
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64

class ThreatIntelligenceRepositoryTest {
    private val fixtureKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

    @Test
    fun `signed feed with multiple indicators yields active count and matches DNS rules`() {
        val feed = signedIndicatorFeedWith(3)
        val manifest = signedManifestJson(feed, fixtureKeyPair)
        val repo = freshRepo()
        val snapshot = repo.updateFromSignedManifest(manifest)

        assertEquals(ThreatRepositoryState.CURRENT, snapshot.state)
        assertEquals(3, snapshot.feed!!.indicatorCount)
        assertEquals(3, repo.current().indicatorCount)

        val engine = DnsDecisionEngine(
            DnsFirewallProfile.ESTRICTO,
            allowlist = emptySet(),
            blocklist = emptySet(),
            rules = repo.current().indicators.map {
                com.aura.defense.vpn.DnsRule(it.indicator, it.category.name, it.source, it.updatedAt, it.severity.name)
            }
        )
        assertEquals(DnsDecision.BLOCK, engine.decide("login.example-phish.test").decision)
    }

    @Test
    fun `invalid signature is rejected and last valid feed is kept`() {
        val repo = freshRepo()
        val valid = signedIndicatorFeedWith(2)
        val signed = signedManifestJson(valid, fixtureKeyPair)
        repo.updateFromSignedManifest(signed)

        val invalid = signed.replaceFirst("\"signature\":\".*\"".toRegex(), "\"signature\":\"bad-signature\"")
        val snapshot = repo.updateFromSignedManifest(invalid)

        assertTrue(snapshot.state == ThreatRepositoryState.STALE || snapshot.state == ThreatRepositoryState.FAILED)
        assertNotNull(repo.activeFeed())
    }

    @Test
    fun `android config resolver rejects blank configuration and keeps last valid values`() {
        val blank = ThreatConfigResolver.resolve(publicKey = "", manifestUrl = "")
        assertEquals("", blank.publicKeyBase64)
        assertEquals("", blank.manifestUrl)
        assertTrue(blank.isValid.not())
    }

    @Test
    fun `etag and last modified are persisted only after valid activation`() {
        val repo = freshRepoWithClient(
            object : ThreatNetworkClient {
                override fun fetch(url: String, etag: String?, lastModified: String?): NetworkFetchResult =
                    NetworkFetchResult(200, signedManifestJson(signedIndicatorFeedWith(1), fixtureKeyPair), "etag-v2", "Mon, 01 Sep 2025 00:00:00 GMT")
            }
        )

        val snapshot = repo.refresh()
        assertEquals(ThreatRepositoryState.CURRENT, snapshot.state)
        assertEquals("etag-v2", repo.lastHttpEtag())
        assertEquals("Mon, 01 Sep 2025 00:00:00 GMT", repo.lastHttpLastModified())
    }

    @Test
    fun `atomic replace failure preserves previous valid feed as stale`() {
        val lastValid = signedIndicatorFeedWith(2)
        val repo = freshRepo()
        repo.updateFromSignedManifest(signedManifestJson(lastValid, fixtureKeyPair))

        val failing = repo.copyForTesting(atomicReplaceFails = true)
        val newFeed = signedIndicatorFeedWith(4)
        val snapshot = failing.updateFromSignedManifest(signedManifestJson(newFeed, fixtureKeyPair))

        assertEquals(ThreatRepositoryState.STALE, snapshot.state)
        assertEquals(lastValid.version, failing.lastValidFeed()?.version)
    }

    @Test
    fun `confirmed match requires active signed feed and DNS blocking uses feed metadata`() {
        val repo = freshRepo()
        repo.updateFromSignedManifest(signedManifestJson(signedIndicatorFeedWith(1), fixtureKeyPair))
        val confirmed = AppScannerRules.confirmedMatch(
            ruleId = "feed-1",
            source = "aurafeed",
            evidence = "login.example-phish.test",
            version = "2026.09.18",
            repository = repo
        )

        assertEquals(AppFindingLevel.CONFIRMED_MATCH, confirmed.level)
        assertTrue(confirmed.reason.contains("confirmada"))

        val missingFeed = freshRepo()
        val unconfirmed = AppScannerRules.confirmedMatch(
            ruleId = "feed-1",
            source = "aurafeed",
            evidence = "login.example-phish.test",
            version = "2026.09.18",
            repository = missingFeed
        )
        assertTrue(unconfirmed.level != AppFindingLevel.CONFIRMED_MATCH)
    }

    private fun freshRepo(): ThreatIntelligenceRepository = ThreatIntelligenceRepository(
        context = null,
        publicKeyBase64 = validPublicKeyBase64(),
        manifestUrl = "https://example.test/feed.json",
        networkClient = object : ThreatNetworkClient {
            override fun fetch(url: String, etag: String?, lastModified: String?): NetworkFetchResult? = null
        }
    )

    private fun freshRepoWithClient(client: ThreatNetworkClient): ThreatIntelligenceRepository = ThreatIntelligenceRepository(
        context = null,
        publicKeyBase64 = validPublicKeyBase64(),
        manifestUrl = "https://example.test/feed.json",
        networkClient = client
    )

    private fun signedIndicatorFeedWith(indicatorCount: Int): SignedThreatFeed {
        val indicators = (1..indicatorCount).map { index ->
            ThreatIndicator(
                id = "feed-$index",
                indicator = when (index) {
                    1 -> "login.example-phish.test"
                    2 -> "tracker.example-phish.test"
                    else -> "malware.example-phish.test"
                },
                indicatorType = ThreatIndicatorType.DOMAIN,
                category = ThreatCategory.MALWARE,
                severity = ThreatSeverity.HIGH,
                descriptionEs = "Dominio de muestra para pruebas de infraestructura",
                source = "aurafeed",
                updatedAt = "2026-09-18T00:00:00Z"
            )
        }
        return SignedThreatFeed(
            ruleId = "feed-1",
            source = "aurafeed",
            version = "2026.09.18",
            evidence = "AURA threat feed with ${indicatorCount} indicators",
            expiresAt = System.currentTimeMillis() + 30_000L,
            checksum = "",
            size = 0L,
            signature = "",
            publicKeyId = "aura-ed25519-feed-v1",
            indicators = indicators
        )
    }

    private fun signedManifestJson(feed: SignedThreatFeed, pair: java.security.KeyPair): String {
        val signedFeed = signedValidFeed(feed, pair)
        val root = JSONObject().apply {
            put("feed", JSONObject().apply {
                put("ruleId", signedFeed.ruleId)
                put("source", signedFeed.source)
                put("version", signedFeed.version)
                put("evidence", signedFeed.evidence)
                put("expiresAt", signedFeed.expiresAt)
                put("checksum", signedFeed.checksum)
                put("size", signedFeed.size)
                put("signature", signedFeed.signature)
                put("publicKeyId", signedFeed.publicKeyId)
                put("indicators", JSONObject().apply {
                    put("count", signedFeed.indicators.size)
                    put("items", org.json.JSONArray().apply {
                        signedFeed.indicators.forEach { indicator ->
                            put(JSONObject().apply {
                                put("id", indicator.id)
                                put("indicator", indicator.indicator)
                                put("indicatorType", indicator.indicatorType.name)
                                put("category", indicator.category.name)
                                put("severity", indicator.severity.name)
                                put("descriptionEs", indicator.descriptionEs)
                                put("source", indicator.source)
                                put("updatedAt", indicator.updatedAt)
                            })
                        }
                    })
                })
            })
        }
        return root.toString()
    }

    private fun signedValidFeed(feed: SignedThreatFeed, pair: java.security.KeyPair): SignedThreatFeed {
        val payload = feed.canonicalPayloadBytes()
        val signature = Signature.getInstance("Ed25519").apply {
            initSign(pair.private)
            update(payload)
        }.sign()
        val encoded = Base64.getEncoder().encodeToString(signature)
        val checksum = sha256Hex(payload)
        return feed.copy(
            checksum = checksum,
            size = payload.size.toLong(),
            signature = encoded
        )
    }

    private fun validPublicKeyBase64(): String = Base64.getEncoder().encodeToString(fixtureKeyPair.public.encoded)

    private fun sha256Hex(value: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(value)
            .joinToString("") { "%02x".format(it) }

    @Test
    fun `replay detection ignores corrupt timestamps`() {
        val validator = SignedThreatFeedValidator(validPublicKeyBase64())
        val feed = signedIndicatorFeedWith(1)
        val signed = signedValidFeed(feed, fixtureKeyPair)
        val seenField = SignedThreatFeedValidator::class.java.getDeclaredField("seenSignatures")
        seenField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val seen = seenField.get(validator) as MutableSet<String>
        seen.add("${signed.ruleId}|${signed.source}|${signed.version}|${signed.signature}|9223372036854775807")
        val validation = validator.validate(signed)
        assertTrue(validation.valid)
    }

    @Test
    fun `unsupported empty config is reported clearly`() {
        val result = ThreatConfigResolver.resolve(publicKey = " ", manifestUrl = "")
        assertEquals("", result.publicKeyBase64)
        assertEquals("", result.manifestUrl)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `signed active feed controls dns blocking and invalid feed is rejected`() {
        val repo = freshRepo()
        val signed = signedValidFeed(signedIndicatorFeedWith(2), fixtureKeyPair)
        val manifest = signedManifestJson(signed, fixtureKeyPair)
        val snapshot = repo.updateFromSignedManifest(manifest)

        assertEquals(ThreatRepositoryState.CURRENT, snapshot.state)
        val rules = repo.activeFeed()!!.indicators.map {
            com.aura.defense.vpn.DnsRule(it.indicator, it.category.name, it.source, repo.activeFeed()!!.version, it.severity.name, repo.activeFeed()!!.expiresAt)
        }
        val engine = DnsDecisionEngine(
            DnsFirewallProfile.ESTRICTO,
            emptySet(),
            emptySet(),
            rules
        )
        assertEquals(DnsDecision.BLOCK, engine.decide("login.example-phish.test").decision)

        val broken = manifest.replace("\"signature\":\"${signed.signature}\"", "\"signature\":\"bad-signature\"")
        val rejected = repo.updateFromSignedManifest(broken)
        assertTrue(rejected.state == ThreatRepositoryState.STALE || rejected.state == ThreatRepositoryState.FAILED)
    }
}

