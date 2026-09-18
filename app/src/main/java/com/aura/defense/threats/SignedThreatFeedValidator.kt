package com.aura.defense.threats

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val DEFAULT_SIGNING_KEY = "aura-local-threat-feed-v1"

data class SignedThreatFeed(
    val ruleId: String,
    val source: String,
    val version: String,
    val evidence: String,
    val expiresAt: Long,
    val signature: String
)

data class SignedThreatValidation(
    val valid: Boolean,
    val reason: String = ""
)

class SignedThreatFeedValidator(
    private val secret: String = DEFAULT_SIGNING_KEY
) {
    fun validate(feed: SignedThreatFeed): SignedThreatValidation {
        if (feed.ruleId.isBlank()) return SignedThreatValidation(false, "missing_rule_id")
        if (feed.source.isBlank()) return SignedThreatValidation(false, "missing_source")
        if (feed.version.isBlank()) return SignedThreatValidation(false, "missing_version")
        if (feed.evidence.isBlank()) return SignedThreatValidation(false, "missing_evidence")
        if (feed.signature.isBlank()) return SignedThreatValidation(false, "missing_signature")
        if (feed.expiresAt <= System.currentTimeMillis()) return SignedThreatValidation(false, "expired_feed")

        val expected = sign(feed)
        val isSignatureValid = constantTimeEquals(feed.signature, expected)
        if (!isSignatureValid) return SignedThreatValidation(false, "invalid_signature")

        return SignedThreatValidation(true, "valid")
    }

    fun sign(feed: SignedThreatFeed): String {
        val payload = listOf(
            feed.ruleId,
            feed.source,
            feed.version,
            feed.evidence,
            feed.expiresAt.toString()
        ).joinToString("|")

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (index in a.indices) diff = diff or (a[index].code xor b[index].code)
        return diff == 0
    }
}
