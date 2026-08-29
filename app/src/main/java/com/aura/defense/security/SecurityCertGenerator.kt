package com.aura.defense.security

import android.content.Context
import android.provider.Settings
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SecurityCertificate(
        val deviceId: String,
            val score: Int,
                val status: String,
                    val totalChecks: Int,
                        val passedChecks: Int,
                            val generatedAt: String,
                                val findings: List<String>,
                                    val integrityPassed: Boolean
) {
        fun toText(): String {
                    val sb = StringBuilder()
                            sb.appendLine("========================================")
                                    sb.appendLine("       AURA DEFENSE CERTIFICATE")
                                            sb.appendLine("       Mobile Cybersecurity Audit")
                                                    sb.appendLine("========================================")
                                                            sb.appendLine()
                                                                    sb.appendLine("Device ID: $deviceId")
                                                                            sb.appendLine("Date: $generatedAt")
                                                                                    sb.appendLine()
                                                                                            sb.appendLine("--- SECURITY SCORE ---")
                                                                                                    sb.appendLine("Score: $score / 100")
                                                                                                            sb.appendLine("Status: $status")
                                                                                                                    sb.appendLine()
                                                                                                                            sb.appendLine("--- INTEGRITY CHECKS ---")
                                                                                                                                    sb.appendLine("Passed: $passedChecks / $totalChecks")
                                                                                                                                            sb.appendLine("Device Integrity: ${if (integrityPassed) "PASSED" else "FAILED"}")
                                                                                                                                                    sb.appendLine()
                                                                                                                                                            if (findings.isNotEmpty()) {
                                                                                                                                                                            sb.appendLine("--- FINDINGS ---")
                                                                                                                                                                                        findings.forEach { sb.appendLine("  - $it") }
                                                                                                                                                                                                    sb.appendLine()
                                                                                                                                                            }
                                                                                                                                                                    sb.appendLine("--- CLASSIFICATION ---")
                                                                                                                                                                            val classification = when {
                                                                                                                                                                                            score >= 90 -> "EXEMPLARY - Excellent security posture"
                                                                                                                                                                                                        score >= 80 -> "SECURE - Good security posture with minor improvements"
                                                                                                                                                                                                                    score >= 60 -> "MODERATE - Action recommended to improve security"
                                                                                                                                                                                                                                score >= 40 -> "VULNERABLE - Significant security gaps detected"
                                                                                                                                                                                                                                            else -> "CRITICAL - Immediate action required"
                                                                                                                                                                            }
                                                                                                                                                                                    sb.appendLine(classification)
                                                                                                                                                                                            sb.appendLine()
                                                                                                                                                                                                    sb.appendLine("========================================")
                                                                                                                                                                                                            sb.appendLine("Generated by Aura Defense")
                                                                                                                                                                                                                    sb.appendLine("First no-root Android cyberdefense platform")
                                                                                                                                                                                                                            sb.appendLine("========================================")
                                                                                                                                                                                                                                    return sb.toString()
        }
}

class SecurityCertGenerator(private val context: Context) {

        fun generate(
                    score: Int,
                            status: String,
                                    findings: List<SecurityFinding>,
                                            integrityResult: IntegrityResult
        ): SecurityCertificate {
                    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            val deviceId = getDeviceId()
                                    val findingTexts = findings.map { "[${it.severity}] ${it.title}" }
                                            return SecurityCertificate(
                                                            deviceId = deviceId,
                                                                        score = score,
                                                                                    status = status,
                                                                                                totalChecks = integrityResult.totalChecks,
                                                                                                            passedChecks = integrityResult.passedCount,
                                                                                                                        generatedAt = ts,
                                                                                                                                    findings = findingTexts,
                                                                                                                                                integrityPassed = integrityResult.overallPassed
                                            )
        }

            private fun getDeviceId(): String {
                        return runCatching {
                                        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                                                    if (androidId != null && androidId.length >= 8) {
                                                                        androidId.take(4) + "****" + androidId.takeLast(4)
                                                    } else {
                                                                        "UNK****UNK"
                                                    }
                        }.getOrElse {
                                        Timber.e(it, "Failed to get device ID")
                                                    "ERR****ERR"
                        }
            }
}
