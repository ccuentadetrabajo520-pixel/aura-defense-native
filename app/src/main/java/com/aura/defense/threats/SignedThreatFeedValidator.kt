package com.aura.defense.threats

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_PUBLIC_KEY_BASE64 = "MCowBQYDK2VwAyEAqNCOkKOvUIIJ0GjysEPW1tpuptXjqSzuuRaNDF7pbpU="
private const val CANONICAL_PAYLOAD_SEPARATOR = "|"

data class ThreatRuleManifest(
    val ruleId: String,
    val source: String,
    val version: String,
    val evidence: String,
    val checksum: String,
    val size: Long,
    val expiresAt: Long
)

object ThreatRuleManifestRegistry {
    private val rules = listOf(
        ThreatRuleManifest(
            ruleId = "aura.rule.local.sample",
            source = "aura-local",
            version = "2026.09.18",
            evidence = "sample-local-rule",
            checksum = sha256Hex(
                listOf(
                    "aura.rule.local.sample",
                    "aura-local",
                    "2026.09.18",
                    "sample-local-rule",
                    Long.MAX_VALUE.toString(),
                    "17"
                ).joinToString(CANONICAL_PAYLOAD_SEPARATOR)
            ),
            size = 17L,
            expiresAt = Long.MAX_VALUE
        )
    )

    fun resolve(ruleId: String, source: String, version: String): ThreatRuleManifest? =
        rules.firstOrNull { it.ruleId == ruleId && it.source == source && it.version == version }

    fun resolveAny(ruleId: String, source: String): ThreatRuleManifest? =
        rules.firstOrNull { it.ruleId == ruleId && it.source == source }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

data class SignedThreatFeed(
    val ruleId: String,
    val source: String,
    val version: String,
    val evidence: String,
    val expiresAt: Long,
    val checksum: String,
    val size: Long,
    val signature: String,
    val publicKeyId: String = "aura-ed25519-feed-v1"
)

data class SignedThreatValidation(
    val valid: Boolean,
    val reason: String = ""
)

class SignedThreatFeedValidator(
    private val publicKeyBase64: String = DEFAULT_PUBLIC_KEY_BASE64,
    private val replayWindowMs: Long = 5 * 60_000L
) {
    private val seenSignatures = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun validate(feed: SignedThreatFeed, expectedRule: ThreatRuleManifest? = null): SignedThreatValidation {
        if (feed.ruleId.isBlank()) return SignedThreatValidation(false, "missing_rule_id")
        if (feed.source.isBlank()) return SignedThreatValidation(false, "missing_source")
        if (feed.version.isBlank()) return SignedThreatValidation(false, "missing_version")
        if (feed.evidence.isBlank()) return SignedThreatValidation(false, "missing_evidence")
        if (feed.signature.isBlank()) return SignedThreatValidation(false, "missing_signature")
        if (feed.expiresAt <= System.currentTimeMillis()) return SignedThreatValidation(false, "expired_feed")
        if (feed.size <= 0L) return SignedThreatValidation(false, "invalid_size")
        if (feed.checksum.isBlank()) return SignedThreatValidation(false, "missing_checksum")

        val actualSize = feed.evidence.toByteArray(Charsets.UTF_8).size.toLong()
        if (feed.size != actualSize) {
            return SignedThreatValidation(false, "size_mismatch")
        }

        val payload = canonicalPayload(feed)
        if (feed.checksum != sha256Hex(payload)) return SignedThreatValidation(false, "checksum_mismatch")

        val manifest = expectedRule ?: ThreatRuleManifestRegistry.resolveAny(feed.ruleId, feed.source)
        if (manifest != null) {
            if (manifest.ruleId != feed.ruleId || manifest.source != feed.source || manifest.version != feed.version) {
                return SignedThreatValidation(false, "rule_version_mismatch")
            }
            if (manifest.expiresAt <= System.currentTimeMillis()) return SignedThreatValidation(false, "manifest_expired")
            if (manifest.expiresAt < feed.expiresAt) return SignedThreatValidation(false, "downgrade_or_replay")
            if (manifest.checksum != feed.checksum || manifest.size != feed.size || manifest.evidence != feed.evidence) {
                return SignedThreatValidation(false, "invalid_manifest_match")
            }
        }

        val key = buildPublicKey()
        val signatureBytes = runCatching { Base64.getDecoder().decode(feed.signature) }.getOrElse { return SignedThreatValidation(false, "invalid_signature_encoding") }
        val sig = runCatching {
            Signature.getInstance("Ed25519").apply {
                initVerify(key)
                update(payload.toByteArray(Charsets.UTF_8))
            }
        }.getOrElse { return SignedThreatValidation(false, "invalid_signature") }
        val signatureValid = runCatching { sig.verify(signatureBytes) }.getOrElse { return SignedThreatValidation(false, "invalid_signature") }
        if (!signatureValid) return SignedThreatValidation(false, "invalid_signature")

        val now = System.currentTimeMillis()
        val replayKey = "${feed.ruleId}|${feed.source}|${feed.version}|${feed.signature}"
        val alreadySeen = seenSignatures.any { item ->
            val parts = item.split("|")
            if (parts.size < 5) return@any false
            val recordedAt = parts[4].toLongOrNull() ?: Long.MAX_VALUE
            parts[0] == feed.ruleId && parts[1] == feed.source && parts[2] == feed.version && parts[3] == feed.signature && (now - recordedAt) < replayWindowMs
        }
        if (alreadySeen) return SignedThreatValidation(false, "replay_detected")
        seenSignatures.add("$replayKey|$now")

        return SignedThreatValidation(true, "valid")
    }

    fun sign(feed: SignedThreatFeed, privateKeyBase64: String): String {
        val privateKey = runCatching { decodeEd25519PrivateKey(privateKeyBase64) }.getOrElse { throw IllegalArgumentException("Clave privada Ed25519 inválida") }
        val payload = canonicalPayload(feed)
        val signer = Signature.getInstance("Ed25519").apply {
            initSign(privateKey)
            update(payload.toByteArray(Charsets.UTF_8))
        }
        return Base64.getEncoder().encodeToString(signer.sign())
    }

    fun validateCanonicalPayload(
        ruleId: String,
        source: String,
        version: String,
        evidence: String,
        expiresAt: Long,
        checksum: String,
        size: Long
    ): String = listOf(
        ruleId,
        source,
        version,
        evidence,
        expiresAt.toString(),
        size.toString()
    ).joinToString(CANONICAL_PAYLOAD_SEPARATOR)

    private fun canonicalPayload(feed: SignedThreatFeed): String = validateCanonicalPayload(
        ruleId = feed.ruleId,
        source = feed.source,
        version = feed.version,
        evidence = feed.evidence,
        expiresAt = feed.expiresAt,
        checksum = feed.checksum,
        size = feed.size
    )

    private fun buildPublicKey() = runCatching {
        val decoded = Base64.getDecoder().decode(publicKeyBase64)
        val spec = X509EncodedKeySpec(decoded)
        KeyFactory.getInstance("Ed25519").generatePublic(spec)
    }.getOrElse { throw IllegalStateException("La clave pública Ed25519 del feed no es válida.", it) }

    private fun decodeEd25519PrivateKey(base64: String) = runCatching {
        val decoded = Base64.getDecoder().decode(base64)
        val spec = PKCS8EncodedKeySpec(decoded)
        KeyFactory.getInstance("Ed25519").generatePrivate(spec)
    }.getOrElse { throw IllegalArgumentException("Clave privada Ed25519 no válida", it) }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
