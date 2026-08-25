package com.aura.defense.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aura.defense.apps.AppScanner
import com.aura.defense.data.DeviceTelemetryProvider
import com.aura.defense.security.SecurityPostureEngine
import com.aura.defense.vault.AuraVault

class AuraCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val telemetry = DeviceTelemetryProvider(applicationContext).read()
        val posture = SecurityPostureEngine().evaluate(telemetry)
        val appSummary = runCatching { AppScanner(applicationContext).scan() }.getOrNull()
        val finalPosture = if (appSummary == null) posture else SecurityPostureEngine().evaluate(telemetry, appSummary.riskyApps.size, appSummary.highRiskApps.size)
        AuraVault(applicationContext).saveSummary("Fecha: ${finalPosture.timestamp}; Estado: ${finalPosture.status}; Puntuación: ${finalPosture.score}")
        Result.success()
    }.getOrElse { Result.failure() }
}
