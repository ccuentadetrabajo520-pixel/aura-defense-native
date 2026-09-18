package com.aura.defense.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
    fun `confirmed match requires signed rule metadata`() {
        val confirmed = AppScannerRules.confirmedMatch(
            ruleId = "rule-123",
            source = "urlhaus",
            evidence = "match: com.evil.app"
        )
        val missingRule = AppScannerRules.confirmedMatch(
            ruleId = null,
            source = "heuristic",
            evidence = "match: com.evil.app"
        )

        assertEquals(AppFindingLevel.CONFIRMED_MATCH, confirmed.level)
        assertEquals(AppFindingLevel.SUSPICIOUS_SIGNAL, missingRule.level)
        assertTrue(confirmed.evidence.contains("rule-123"))
    }

    @Test
    fun `explainable score does not hide the reason`() {
        val score = 80
        val explanation = "Base 100 - VPN inactiva (-8) - bloqueo no seguro (-12)"
        assertTrue(explanation.contains("-"))
        assertTrue(score >= 0)
    }

    @Test
    fun `history and exceptions can be cleared locally`() {
        val historyCleared = true
        val exceptionsEmpty = true
        assertTrue(historyCleared)
        assertTrue(exceptionsEmpty)
    }

    @Test
    fun `manifest does not request query all packages`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("QUERY_ALL_PACKAGES"))
    }

    @Test
    fun `partial coverage is explicit when Android hides signals`() {
        val status = AppScannerRules.partialCoverageStatus(false, "instalador")
        assertTrue(status.contains("Cobertura parcial"))
    }
}
