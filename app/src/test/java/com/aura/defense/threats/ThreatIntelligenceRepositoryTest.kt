package com.aura.defense.threats

import com.aura.defense.apps.AppFindingLevel
import com.aura.defense.apps.AppScannerRules
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64

class ThreatIntelligenceRepositoryTest {
    private val fixtureKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

    @Test
    fun `valid signed manifest updates active feed`() {
        val repo = freshRepo()
        val raw = signedManifestJson(validFeed(), fixtureKeyPair)
        val feed = repo.updateFromSignedManifest(raw)
        val validator = SignedThreatFeedValidator(validPublicKeyBase64())
        val parsed = JSONObject(raw)
        val signed = SignedThreatFeed(
            ruleId = parsed.optString("ruleId"),
            source = parsed.optString("source"),
            version = parsed.optString("version"),
            evidence = parsed.optString("evidence"),
            expiresAt = parsed.optLong("expiresAt"),
            checksum = parsed.optString("checksum"),
            size = parsed.optLong("size"),
            signature = parsed.optString("signature"),
            publicKeyId = parsed.optString("publicKeyId")
        )
        val validation = validator.validate(signed)
        System.err.println("DEBUG valid snapshot=${feed.state} reason=${feed.reason} feed=${feed.feed} validation=${validation}")

        assertEquals(ThreatRepositoryState.CURRENT, feed.state)
        assertNotNull(feed.feed)
        assertEquals("rule-001", feed.feed!!.ruleId)
    }

    @Test
    fun `invalid signature is rejected`() {
        val manifest = signedManifestJson(validFeed(), fixtureKeyPair)
        val invalid = manifest.replaceFirst("\"signature\":\".*\"".toRegex(), "\"signature\":\"bad-signature\"")
        val repo = freshRepo()
        repo.updateFromSignedManifest(manifest)
        val snapshot = repo.updateFromSignedManifest(invalid)

        assertTrue(snapshot.state == ThreatRepositoryState.STALE || snapshot.state == ThreatRepositoryState.FAILED)
    }

    @Test
    fun `malformed replay timestamps are ignored instead of causing false replay detection`() {
        val validator = SignedThreatFeedValidator(validPublicKeyBase64())
        val feed = signedValidFeed(validFeed(), fixtureKeyPair)

        val seenField = SignedThreatFeedValidator::class.java.getDeclaredField("seenSignatures")
        seenField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val seenSignatures = seenField.get(validator) as MutableSet<String>
        seenSignatures.add("${feed.ruleId}|${feed.source}|${feed.version}|${feed.signature}|9223372036854775807")

        val validation = validator.validate(feed)
        assertTrue("Timestamps corruptos no deben disparar un replay falso", validation.valid)
    }

    @Test
    fun `confirmed match requires active valid feed`() {
        val repo = repoWithSignedManifest(validFeed())
        val confirmed = AppScannerRules.confirmedMatch(
            ruleId = "rule-001",
            source = "aurafeed",
            evidence = "rule-001",
            version = "1.0.0",
            repository = repo
        )

        assertEquals(AppFindingLevel.CONFIRMED_MATCH, confirmed.level)

        val repoWithoutFeed = ThreatIntelligenceRepository(
            context = null,
            publicKeyBase64 = validPublicKeyBase64(),
            manifestUrl = "https://example.test/feed.json",
            networkClient = object : ThreatNetworkClient {
                override fun fetch(url: String, etag: String?, lastModified: String?): NetworkFetchResult? = null
            }
        )
        val unconfirmed = AppScannerRules.confirmedMatch(
            ruleId = "rule-001",
            source = "aurafeed",
            evidence = "rule-001",
            version = "1.0.0",
            repository = repoWithoutFeed
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

    private fun repoWithSignedManifest(feed: SignedThreatFeed): ThreatIntelligenceRepository {
        val repo = freshRepo()
        repo.updateFromSignedManifest(signedManifestJson(feed, fixtureKeyPair))
        return repo
    }

    private fun validFeed(): SignedThreatFeed = SignedThreatFeed(
        ruleId = "rule-001",
        source = "aurafeed",
        version = "1.0.0",
        evidence = "rule-001",
        expiresAt = System.currentTimeMillis() + 30_000L,
        checksum = "",
        size = 0L,
        signature = "",
        publicKeyId = "aura-ed25519-feed-v1"
    )

    private fun signedValidFeed(feed: SignedThreatFeed, pair: java.security.KeyPair): SignedThreatFeed {
        val effectiveSize = feed.evidence.toByteArray(Charsets.UTF_8).size.toLong()
        val payload = listOf(
            feed.ruleId,
            feed.source,
            feed.version,
            feed.evidence,
            feed.expiresAt.toString(),
            effectiveSize.toString()
        ).joinToString("|")
        val signature = Signature.getInstance("Ed25519").apply {
            initSign(pair.private)
            update(payload.toByteArray(Charsets.UTF_8))
        }.sign()
        val encodedSignature = Base64.getEncoder().encodeToString(signature)
        val checksum = sha256Hex(payload)
        return feed.copy(
            checksum = checksum,
            size = effectiveSize,
            signature = encodedSignature
        )
    }

    private fun validPublicKeyBase64(): String = Base64.getEncoder().encodeToString(fixtureKeyPair.public.encoded)

    private fun signedManifestJson(feed: SignedThreatFeed, pair: java.security.KeyPair): String {
        val signedFeed = signedValidFeed(feed, pair)
        return JSONObject().apply {
            put("ruleId", signedFeed.ruleId)
            put("source", signedFeed.source)
            put("version", signedFeed.version)
            put("evidence", signedFeed.evidence)
            put("expiresAt", signedFeed.expiresAt)
            put("checksum", signedFeed.checksum)
            put("size", signedFeed.size)
            put("signature", signedFeed.signature)
            put("publicKeyId", signedFeed.publicKeyId)
        }.toString()
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
