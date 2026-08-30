package com.aura.defense.security

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AppPrivacyInfo(
        val packageName: String,
            val appName: String,
                val hasCamera: Boolean,
                    val hasMicrophone: Boolean,
                        val hasLocation: Boolean,
                            val hasContacts: Boolean,
                                val hasSms: Boolean,
                                    val hasOverlay: Boolean,
                                        val lastUsed: String,
                                            val sensitiveCount: Int,
                                                val riskLevel: String
)

data class PrivacyAuditResult(
        val apps: List<AppPrivacyInfo>,
            val totalAppsAudited: Int,
                val highRiskCount: Int,
                    val warnings: List<String>,
                        val timestamp: String
)

class PrivacyAuditor(private val context: Context) {

        fun audit(): PrivacyAuditResult {
                    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            val warnings = mutableListOf<String>()
                                    val pm = context.packageManager
                                            val usageStats = runCatching {
                                                            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                                            }.getOrNull()

                                                    val apps = runCatching {
                                                                    val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                                                        pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
                                                                    } else {
                                                                                        @Suppress("DEPRECATION")
                                                                                                        pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                                                                    }
                                                                                packages.filter { (it.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM == 0 }.mapNotNull { pkg ->
                                                                                                val info = pkg.applicationInfo ?: return@mapNotNull null
                                                                                                                val name = runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(pkg.packageName)
                                                                                                                                val perms = pkg.requestedPermissions?.toList().orEmpty()
                                                                                                                                                val flags = pkg.requestedPermissionsFlags
                                                                                                                                                
                                                                                                                                                                val hasCam = hasPerm(perms, flags, "android.permission.CAMERA")
                                                                                                                                                                                val hasMic = hasPerm(perms, flags, "android.permission.RECORD_AUDIO")
                                                                                                                                                                                                val hasLoc = hasPerm(perms, flags, "android.permission.ACCESS_FINE_LOCATION") || hasPerm(perms, flags, "android.permission.ACCESS_COARSE_LOCATION")
                                                                                                                                                                                                                val hasCon = hasPerm(perms, flags, "android.permission.READ_CONTACTS")
                                                                                                                                                                                                                                val hasSms = hasPerm(perms, flags, "android.permission.READ_SMS") || hasPerm(perms, flags, "android.permission.RECEIVE_SMS")
                                                                                                                                                                                                                                                val hasOvl = hasPerm(perms, flags, "android.permission.SYSTEM_ALERT_WINDOW")
                                                                                                                                                                                                                                
                                                                                                                                                                                                                                                                val count = listOf(hasCam, hasMic, hasLoc, hasCon, hasSms, hasOvl).count { it }
                                                                                                                                                                                                                                                                                val risk = when {
                                                                                                                                                                                                                                                                                                        count >= 5 -> "CRITICO"
                                                                                                                                                                                                                                                                                                                            count >= 3 -> "ALTO"
                                                                                                                                                                                                                                                                                                                                                count >= 1 -> "MEDIO"
                                                                                                                                                                                                                                                                                                                                                                    else -> "BAJO"
                                                                                                                                                                                                                                                                                                                                                }
                                                                                                                                                                                                                                                                                                if (risk == "BAJO") return@mapNotNull null

                                                                                                                                                                                                                                                                                                                val lastUsed = getLastUsed(usageStats, pkg.packageName)
                                                                                                                                                                                                                                                                                                                                AppPrivacyInfo(pkg.packageName, name, hasCam, hasMic, hasLoc, hasCon, hasSms, hasOvl, lastUsed, count, risk)
                                                                                }.sortedByDescending { it.sensitiveCount }
                                                    }.onFailure { e ->
                                                                Timber.e(e, "Privacy audit failed")
                                                                    }.getOrDefault(emptyList())

                                                                                            val highRisk = apps.count { it.riskLevel == "CRITICO" || it.riskLevel == "ALTO" }
                                                                                                    if (highRisk > 0) warnings.add("$highRisk app(s) con acceso a combinaciones de permisos sensibles de alto riesgo.")
                                                                                                            val withOverlay = apps.count { it.hasOverlay }
                                                                                                                    if (withOverlay > 0) warnings.add("$withOverlay app(s) pueden dibujar sobre otras ventanas (overlay).")

                                                                                                                            return PrivacyAuditResult(apps, apps.size, highRisk, warnings, ts)
        }

            private fun hasPerm(perms: List<String>, flags: IntArray?, perm: String): Boolean {
                        val idx = perms.indexOf(perm)
                                if (idx < 0 || flags == null || idx >= flags.size) return false
                                        return (flags[idx] and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            }

                private fun getLastUsed(usageStats: UsageStatsManager?, packageName: String): String {
                            if (usageStats == null) return "Desconocido"
                                    return runCatching {
                                                    val now = System.currentTimeMillis()
                                                                val events = usageStats.queryEvents(now - TimeUnit.DAYS.toMillis(7), now)
                                                                            var lastTime = 0L
                                                                                        val event = UsageEvents.Event()
                                                                                                    while (events.hasNextEvent()) {
                                                                                                                        events.getNextEvent(event)
                                                                                                                                        if (event.packageName == packageName && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && event.timeStamp > lastTime) {
                                                                                                                                                                lastTime = event.timeStamp
                                                                                                                                        }
                                                                                                    }
                                                                                                                if (lastTime > 0) {
                                                                                                                                    val diff = now - lastTime
                                                                                                                                                    when {
                                                                                                                                                                            diff < TimeUnit.MINUTES.toMillis(60) -> "Hace ${TimeUnit.MILLISECONDS.toMinutes(diff)} min"
                                                                                                                                                                                                diff < TimeUnit.HOURS.toMillis(24) -> "Hace ${TimeUnit.MILLISECONDS.toHours(diff)} h"
                                                                                                                                                                                                                    diff < TimeUnit.DAYS.toMillis(7) -> "Hace ${TimeUnit.MILLISECONDS.toDays(diff)} d"
                                                                                                                                                                                                                                        else -> "Mas de 7 dias"
                                                                                                                                                    }
                                                                                                                } else "Sin uso reciente"
                                    }.getOrDefault("Desconocido")
                }
}
