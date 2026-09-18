package com.aura.defense.apps

import com.aura.defense.threats.SignedThreatFeed
import com.aura.defense.threats.SignedThreatFeedValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AppScannerRulesTest {

    private val validator = SignedThreatFeedValidator("aura-test-key")

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
        val validFeed = signedFeed(
            ruleId = "rule-123",
            source = "urlhaus",
            version = "2026.09.18",
            evidence = "match: com.evil.app",
            expiresAt = System.currentTimeMillis() + 60_000L
        )

        val valid = validator.validate(validFeed)
        val missingRule = validator.validate(validFeed.copy(ruleId = ""))
        val invalidSignature = validator.validate(validFeed.copy(signature = "bad-signature"))
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
        signature: String = signRule(ruleId, source, version, evidence, expiresAt)
    ) = SignedThreatFeed(
        ruleId = ruleId,
        source = source,
        version = version,
        evidence = evidence,
        expiresAt = expiresAt,
        signature = signature
    )

    private fun signRule(ruleId: String, source: String, version: String, evidence: String, expiresAt: Long): String {
        val payload = listOf(ruleId, source, version, evidence, expiresAt.toString()).joinToString("|")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec("aura-test-key".toByteArray(), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
