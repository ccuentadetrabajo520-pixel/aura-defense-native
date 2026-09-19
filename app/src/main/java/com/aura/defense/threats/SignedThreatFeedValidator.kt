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
    val indicators: List<ThreatIndicator> = emptyList(),
    val feedTimestamp: Long = System.currentTimeMillis()
) {
    val indicatorCount: Int
        get() = indicators.size

    fun canonicalPayloadString(): String = ThreatFeedCanonicalizer.canonicalize(this)

    fun canonicalPayloadBytes(): ByteArray = canonicalPayloadString().toByteArray(Charsets.UTF_8)
}

object ThreatFeedCanonicalizer {
    fun canonicalize(feed: SignedThreatFeed): String {
        val indicatorJson = feed.indicators.sortedBy { it.id }.joinToString(",") { indicator ->
            buildString {
                append("{")
                append("\"id\":\"").append(escapeJson(indicator.id)).append("\",")
                append("\"indicator\":\"").append(escapeJson(indicator.indicator)).append("\",")
                append("\"indicatorType\":\"").append(escapeJson(indicator.indicatorType.name)).append("\",")
                append("\"category\":\"").append(escapeJson(indicator.category.name)).append("\",")
                append("\"severity\":\"").append(escapeJson(indicator.severity.name)).append("\",")
                append("\"descriptionEs\":\"").append(escapeJson(indicator.descriptionEs)).append("\",")
                append("\"source\":\"").append(escapeJson(indicator.source)).append("\",")
                append("\"updatedAt\":\"").append(escapeJson(indicator.updatedAt)).append("\"")
                append("}")
            }
        }

        return buildString {
            append("{")
            append("\"ruleId\":\"").append(escapeJson(feed.ruleId)).append("\",")
            append("\"source\":\"").append(escapeJson(feed.source)).append("\",")
            append("\"version\":\"").append(escapeJson(feed.version)).append("\",")
            append("\"evidence\":\"").append(escapeJson(feed.evidence)).append("\",")
            append("\"expiresAt\":").append(feed.expiresAt).append(",")
            append("\"indicators\":[").append(indicatorJson).append("]")
            append("}")
        }
    }

    private fun escapeJson(value: String): String = buildString {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (ch.code < 0x20) append("\\u")
                        .append(ch.code.toString().padStart(4, '0'))
                    else append(ch)
                }
            }
        }
    }
}

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
        if (feed.ruleId.isBlank() || feed.ruleId.contains("|") || feed.ruleId.contains(";;")) return SignedThreatValidation(false, "missing_rule_id")
        if (feed.source.isBlank() || feed.source.contains("|") || feed.source.contains(";;")) return SignedThreatValidation(false, "missing_source")
        if (feed.version.isBlank() || feed.version.contains("|") || feed.version.contains(";;")) return SignedThreatValidation(false, "missing_version")
        if (feed.evidence.isBlank() || feed.evidence.contains("|") || feed.evidence.contains(";;")) return SignedThreatValidation(false, "missing_evidence")
        if (feed.signature.isBlank()) return SignedThreatValidation(false, "missing_signature")
        if (feed.expiresAt <= System.currentTimeMillis()) return SignedThreatValidation(false, "expired_feed")
        if (feed.size <= 0L) return SignedThreatValidation(false, "invalid_size")
        if (feed.checksum.isBlank()) return SignedThreatValidation(false, "missing_checksum")
        if (feed.size != feed.canonicalPayloadBytes().size.toLong()) return SignedThreatValidation(false, "size_mismatch")

        val payload = feed.canonicalPayloadString()
        if (feed.checksum != sha256Hex(payload)) return SignedThreatValidation(false, "checksum_mismatch")

        val indicatorIds = mutableSetOf<String>()
        for (indicator in feed.indicators) {
            if (indicator.id.isBlank() || indicator.id.contains("|") || indicator.id.contains(";;")) return SignedThreatValidation(false, "invalid_indicator_id")
            if (indicator.indicator.isBlank()) return SignedThreatValidation(false, "invalid_indicator_value")
            if (indicator.indicator.contains("|") || indicator.indicator.contains(";;")) return SignedThreatValidation(false, "invalid_indicator_delimiter")
            if (!indicatorIds.add(indicator.id)) return SignedThreatValidation(false, "duplicate_indicator")
            if (indicator.indicatorType == ThreatIndicatorType.DOMAIN && com.aura.defense.vpn.DnsDecisionEngine.normalize(indicator.indicator) == null) {
                return SignedThreatValidation(false, "invalid_domain")
            }
            runCatching { ThreatCategory.valueOf(indicator.category.name) }
                .getOrElse { return SignedThreatValidation(false, "invalid_category") }
            runCatching { ThreatSeverity.valueOf(indicator.severity.name) }
                .getOrElse { return SignedThreatValidation(false, "invalid_severity") }
            if (indicator.updatedAt.isBlank() || runCatching { java.time.Instant.parse(indicator.updatedAt) }.isFailure) {
                return SignedThreatValidation(false, "invalid_updated_at")
            }
        }

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
    ): String {
        val root = linkedMapOf(
            "ruleId" to ruleId,
            "source" to source,
            "version" to version,
            "evidence" to evidence,
            "expiresAt" to expiresAt,
            "indicators" to emptyList<Any>()
        )
        return ThreatFeedCanonicalizer.canonicalize(
            SignedThreatFeed(
                ruleId = ruleId,
                source = source,
                version = version,
                evidence = evidence,
                expiresAt = expiresAt,
                checksum = checksum,
                size = size,
                signature = "",
                publicKeyId = "aura-ed25519-feed-v1",
                indicators = emptyList()
            )
        )
    }

    private fun buildPublicKey() = runCatching {
        val decoded = Base64.getDecoder().decode(publicKeyBase64)
        val spec = X509EncodedKeySpec(decoded)
        KeyFactory.getInstance("Ed25519").generatePublic(spec)
    }.getOrElse { throw IllegalStateException("La clave pública Ed25519 del feed no es válida.", it) }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
