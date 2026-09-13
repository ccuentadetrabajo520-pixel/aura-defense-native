package com.aura.defense.scheduler

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

enum class AuraSchedule { APAGADO, DIARIO, SEMANAL }

fun scheduleAuraChecks(context: Context, schedule: AuraSchedule) = runCatching {
    val manager = WorkManager.getInstance(context)
    manager.cancelUniqueWork(WORK_NAME)
    if (schedule != AuraSchedule.APAGADO) {
        val days = if (schedule == AuraSchedule.DIARIO) 1L else 7L
        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, PeriodicWorkRequestBuilder<AuraCheckWorker>(days, TimeUnit.DAYS).build())
    }
    true
}.getOrDefault(false)

private const val WORK_NAME = "aura_guardian_checks"

fun ensureScheduled(context: Context) = runCatching {
    val manager = WorkManager.getInstance(context)
    val existing = manager.getWorkInfosForUniqueWork(WORK_NAME).get()
    if (existing.none {
            it.state == androidx.work.WorkInfo.State.ENQUEUED ||
                it.state == androidx.work.WorkInfo.State.RUNNING
        }) {
        manager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AuraCheckWorker>(1, TimeUnit.DAYS).build()
        )
    }
    true
}.getOrDefault(false)
