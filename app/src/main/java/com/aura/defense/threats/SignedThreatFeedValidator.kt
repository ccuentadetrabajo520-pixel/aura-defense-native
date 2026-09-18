package com.aura.defense.threats

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

private const val CANONICAL_PAYLOAD_SEPARATOR = "|"

data class SignedThreatFeed(
    val ruleId: String,
    val source: String,
    val version: String,
    val evidence: String,
    val expiresAt: Long,
    val checksum: String,
    val size: Long,
    val signature: String,
    val publicKeyId: String = "aura-ed25519-feed-v1",
    val feedTimestamp: Long = System.currentTimeMillis()
)

data class SignedThreatValidation(
    val valid: Boolean,
    val reason: String = ""
)

class SignedThreatFeedValidator(
    private val publicKeyBase64: String,
    private val replayWindowMs: Long = 5 * 60_000L
) {
    private val seenSignatures = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun validate(feed: SignedThreatFeed): SignedThreatValidation {
        if (publicKeyBase64.isBlank()) return SignedThreatValidation(false, "missing_public_key")
        if (feed.ruleId.isBlank()) return SignedThreatValidation(false, "missing_rule_id")
        if (feed.source.isBlank()) return SignedThreatValidation(false, "missing_source")
        if (feed.version.isBlank()) return SignedThreatValidation(false, "missing_version")
        if (feed.evidence.isBlank()) return SignedThreatValidation(false, "missing_evidence")
        if (feed.signature.isBlank()) return SignedThreatValidation(false, "missing_signature")
        if (feed.expiresAt <= System.currentTimeMillis()) return SignedThreatValidation(false, "expired_feed")
        if (feed.size <= 0L) return SignedThreatValidation(false, "invalid_size")
        if (feed.checksum.isBlank()) return SignedThreatValidation(false, "missing_checksum")

        val actualSize = feed.evidence.toByteArray(Charsets.UTF_8).size.toLong()
        if (feed.size != actualSize) return SignedThreatValidation(false, "size_mismatch")

        val payload = canonicalPayload(feed)
        if (feed.checksum != sha256Hex(payload)) return SignedThreatValidation(false, "checksum_mismatch")

        val key = buildPublicKey()
        val signatureBytes = runCatching { Base64.getDecoder().decode(feed.signature) }.getOrElse {
            return SignedThreatValidation(false, "invalid_signature_encoding")
        }
        val sig = runCatching {
            Signature.getInstance("Ed25519").apply {
                initVerify(key)
                update(payload.toByteArray(Charsets.UTF_8))
            }
        }.getOrElse { return SignedThreatValidation(false, "invalid_signature") }
        val signatureValid = runCatching { sig.verify(signatureBytes) }.getOrElse {
            return SignedThreatValidation(false, "invalid_signature")
        }
        if (!signatureValid) return SignedThreatValidation(false, "invalid_signature")

        val replayKey = "${feed.ruleId}|${feed.source}|${feed.version}|${feed.signature}"
        val now = System.currentTimeMillis()
        val alreadySeen = seenSignatures.any { item ->
            val parts = item.split("|")
            if (parts.size < 5) return@any false
            val recordedAt = parts[4].toLongOrNull() ?: return@any false
            if (recordedAt < 0L || recordedAt > now + replayWindowMs) return@any false
            parts[0] == feed.ruleId && parts[1] == feed.source && parts[2] == feed.version && parts[3] == feed.signature && (now - recordedAt) < replayWindowMs
        }
        if (alreadySeen) return SignedThreatValidation(false, "replay_detected")
        seenSignatures.add("$replayKey|$now")

        return SignedThreatValidation(true, "valid")
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

    private fun canonicalPayload(feed: SignedThreatFeed): String = listOf(
        feed.ruleId,
        feed.source,
        feed.version,
        feed.evidence,
        feed.expiresAt.toString(),
        feed.size.toString()
    ).joinToString(CANONICAL_PAYLOAD_SEPARATOR)

    private fun buildPublicKey() = runCatching {
        val decoded = Base64.getDecoder().decode(publicKeyBase64)
        val spec = X509EncodedKeySpec(decoded)
        KeyFactory.getInstance("Ed25519").generatePublic(spec)
    }.getOrElse { throw IllegalStateException("La clave pública Ed25519 del feed no es válida.", it) }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
