package com.aura.defense.apps

import com.aura.defense.threats.SignedThreatFeed
import com.aura.defense.threats.SignedThreatFeedValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64

class AppScannerRulesTest {

    @Test
    fun `name heuristics never generate critical malware verdict`() {
        val finding = AppScannerRules.nameHeuristicOrNull("com.spy.phone", "Spy Phone")

        assertEquals(AppFindingLevel.SUSPICIOUS_SIGNAL, finding!!.level)
        assertNotEquals(AppRiskSeverity.HIGH, finding.severity)
    }

    @Test
    fun `common permissions never classify as malware`() {
        val findings = AppScannerRules.permissionFindings(listOf(
            "android.permission.CAMERA",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.RECORD_AUDIO"
        ))

        assertTrue(findings.isNotEmpty())
        assertTrue(findings.all { it.level != AppFindingLevel.CONFIRMED_MATCH })
        assertTrue(findings.all { it.level != AppFindingLevel.HIGH_RISK })
    }

    @Test
    fun `confirmed match requires signed active feed and valid rule metadata`() {
        val pair = newEd25519KeyPair()
        val validator = SignedThreatFeedValidator(publicKeyBase64 = Base64.getEncoder().encodeToString(pair.public.encoded))
        val expiresAt = System.currentTimeMillis() + 60_000L
        val evidence = "rule-remote-01"
        val size = evidence.toByteArray(Charsets.UTF_8).size.toLong()
        val ruleId = "com.example.rule.remote_01"
        val source = "external-threat-feed"
        val version = "2026.09.18"
        val checksum = sha256Hex(validator.validateCanonicalPayload(
            ruleId = ruleId,
            source = source,
            version = version,
            evidence = evidence,
            expiresAt = expiresAt,
            checksum = "ignored",
            size = size
        ))
        val validFeed = signedFeed(
            ruleId = ruleId,
            source = source,
            version = version,
            evidence = evidence,
            expiresAt = expiresAt,
            checksum = checksum,
            size = size,
            privateKey = pair.private
        )

        val valid = validator.validate(validFeed)
        val missingRule = validator.validate(validFeed.copy(ruleId = ""))
        val invalidSignature = validator.validate(validFeed.copy(signature = Base64.getEncoder().encodeToString(ByteArray(64) { 7 })))
        val expired = validator.validate(validFeed.copy(expiresAt = System.currentTimeMillis() - 1_000L))

        assertTrue(valid.valid)
        assertFalse(missingRule.valid)
        assertFalse(invalidSignature.valid)
        assertFalse(expired.valid)
    }

    @Test
    fun `manifest does not request query all packages`() {
        val manifestPath = listOf(
            File(System.getProperty("user.dir"), "app/src/main/AndroidManifest.xml"),
            File(System.getProperty("user.dir"), "src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml")
        ).firstOrNull { it.exists() }

        assertTrue("No se encontró el AndroidManifest.xml del módulo app", manifestPath != null)
        val manifest = manifestPath!!.readText()
        assertFalse(manifest.contains("QUERY_ALL_PACKAGES"))
    }

    @Test
    fun `partial coverage is explicit when Android hides signals`() {
        val status = AppScannerRules.partialCoverageStatus(false, "instalador")
        assertTrue(status.contains("Cobertura parcial"))
    }

    private fun signedFeed(
        ruleId: String,
        source: String,
        version: String,
        evidence: String,
        expiresAt: Long,
        checksum: String,
        size: Long,
        privateKey: java.security.PrivateKey
    ): SignedThreatFeed {
        val canonical = listOf(ruleId, source, version, evidence, expiresAt.toString(), size.toString()).joinToString("|")
        val signature = Signature.getInstance("Ed25519").apply {
            initSign(privateKey)
            update(canonical.toByteArray(Charsets.UTF_8))
        }.sign()
        return SignedThreatFeed(
            ruleId = ruleId,
            source = source,
            version = version,
            evidence = evidence,
            expiresAt = expiresAt,
            checksum = checksum,
            size = size,
            signature = Base64.getEncoder().encodeToString(signature),
            publicKeyId = "aura-ed25519-feed-v1"
        )
    }

    private fun newEd25519KeyPair(): KeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
