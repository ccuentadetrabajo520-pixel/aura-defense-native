package com.aura.defense.history

import com.aura.defense.apps.AppScanResult
import com.aura.defense.security.PostureResult

 data class AuraBaseline(
    val timestamp: String,
    val score: Int,
    val vpnActive: Boolean,
    val privateDnsStatus: String,
    val apps: Map<String, AppSnapshot>
) {
    companion object {
        fun from(posture: PostureResult, appScan: AppScanResult?): AuraBaseline = AuraBaseline(
            timestamp = posture.timestamp,
            score = posture.score,
            vpnActive = posture.telemetry.vpnActive,
            privateDnsStatus = posture.telemetry.privateDnsStatus,
            apps = appScan?.apps.orEmpty().associate { app ->
                app.packageName to AppSnapshot(app.versionName, app.targetSdk, app.requestedPermissions.sorted(), app.installerPackage, app.findings.map { it.severity.name }.sorted())
            }
        )
    }
}

data class AppSnapshot(
    val versionName: String,
    val targetSdk: Int,
    val permissions: List<String>,
    val installer: String,
    val findings: List<String>
)
