package com.aura.defense.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PermissionRisk { NORMAL, SENSITIVE, DANGEROUS, CRITICAL }

data class PermissionItem(val name: String, val risk: PermissionRisk, val granted: Boolean)

data class PermissionAuditEntry(
        val packageName: String,
            val appName: String,
                val permissions: List<PermissionItem>,
                    val riskLevel: PermissionRisk,
                        val reason: String
)

data class PermissionAuditResult(
        val entries: List<PermissionAuditEntry>,
            val totalApps: Int,
                val appsWithDangerousPermissions: Int,
                    val timestamp: String
)

class PermissionAuditor(private val context: Context) {

        companion object {
                    private val DANGEROUS = setOf(
                                    "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS",
                                                "android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG",
                                                            "android.permission.READ_PHONE_STATE", "android.permission.CALL_PHONE",
                                                                        "android.permission.READ_SMS", "android.permission.RECEIVE_SMS",
                                                                                    "android.permission.SEND_SMS", "android.permission.RECEIVE_MMS",
                                                                                                "android.permission.BODY_SENSORS", "android.permission.READ_CALENDAR",
                                                                                                            "android.permission.WRITE_CALENDAR", "android.permission.CAMERA",
                                                                                                                        "android.permission.RECORD_AUDIO", "android.permission.ACCESS_FINE_LOCATION",
                                                                                                                                    "android.permission.ACCESS_COARSE_LOCATION", "android.permission.READ_EXTERNAL_STORAGE",
                                                                                                                                                "android.permission.WRITE_EXTERNAL_STORAGE"
                    )
                            private val SENSITIVE = setOf(
                                            "android.permission.GET_ACCOUNTS", "android.permission.READ_PROFILE",
                                                        "android.permission.SYSTEM_ALERT_WINDOW", "android.permission.REQUEST_INSTALL_PACKAGES",
                                                                    "android.permission.NOTIFICATION_LISTENER_SERVICE", "android.permission.BIND_ACCESSIBILITY_SERVICE",
                                                                                "android.permission.QUERY_ALL_PACKAGES"
                            )
                                    private val SUSPICIOUS_COMBOS = listOf(
                                                    setOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.ACCESS_FINE_LOCATION"),
                                                                setOf("android.permission.READ_SMS", "android.permission.RECEIVE_SMS", "android.permission.READ_CONTACTS"),
                                                                            setOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.READ_CALL_LOG")
                                    )
        }

            fun audit(): PermissionAuditResult {
                        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                return runCatching {
                                                val pm = context.packageManager
                                                            val packages = getInstalledPackages(pm)
                                                                        val entries = packages.mapNotNull { analyzePackage(pm, it) }
                                                                                    PermissionAuditResult(
                                                                                                        entries = entries.sortedByDescending { it.riskLevel },
                                                                                                                        totalApps = packages.size,
                                                                                                                                        appsWithDangerousPermissions = entries.count { it.riskLevel >= PermissionRisk.DANGEROUS },
                                                                                                                                                        timestamp = ts
                                                                                    )
                                }.onFailure { Timber.e(it, "Permission audit failed") }
                                            .getOrElse { PermissionAuditResult(emptyList(), 0, 0, ts) }
            }

                private fun getInstalledPackages(pm: PackageManager): List<PackageInfo> = runCatching {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
                            } else {
                                            @Suppress("DEPRECATION")
                                                        pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                            }
                }.getOrElse {
                            @Suppress("DEPRECATION")
                                    pm.getInstalledPackages(0)
                }

                    private fun analyzePackage(pm: PackageManager, pkgInfo: PackageInfo): PermissionAuditEntry? {
                                val appInfo = pkgInfo.applicationInfo ?: return null
                                        if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return null
                                                val appName = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault(pkgInfo.packageName)
                                                        val requested = pkgInfo.requestedPermissions?.toList().orEmpty()
                                                                val items = requested.mapIndexedNotNull { index, name ->
                                                                            val flags = pkgInfo.requestedPermissionsFlags?.getOrNull(index) ?: 0
                                                                                        val granted = (flags and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                                                                                                    val risk = when {
                                                                                                                        SENSITIVE.contains(name) -> PermissionRisk.SENSITIVE
                                                                                                                                        DANGEROUS.contains(name) -> PermissionRisk.DANGEROUS
                                                                                                                                                        else -> return@mapIndexedNotNull null
                                                                                                    }
                                                                                                                PermissionItem(name = name, risk = risk, granted = granted)
                                                                }
                                                                        val grantedSet = items.filter { it.granted }.map { it.name }.toSet()
                                                                                val matchedCombos = SUSPICIOUS_COMBOS.filter { combo -> combo.all { it in grantedSet } }
                                                                                        val dangerCount = items.count { it.risk == PermissionRisk.DANGEROUS && it.granted }
                                                                                                val riskLevel = when {
                                                                                                                matchedCombos.isNotEmpty() -> PermissionRisk.CRITICAL
                                                                                                                            dangerCount >= 5 -> PermissionRisk.CRITICAL
                                                                                                                                        dangerCount >= 3 -> PermissionRisk.DANGEROUS
                                                                                                                                                    dangerCount > 0 -> PermissionRisk.SENSITIVE
                                                                                                                                                                else -> PermissionRisk.NORMAL
                                                                                                }
                                                                                                        val reason = when {
                                                                                                                        matchedCombos.isNotEmpty() -> "Combinacion sospechosa de permisos detectada"
                                                                                                                                    dangerCount >= 5 -> "$dangerCount permisos peligrosos otorgados"
                                                                                                                                                dangerCount >= 3 -> "$dangerCount permisos peligrosos otorgados"
                                                                                                                                                            dangerCount > 0 -> "Tiene permisos peligrosos activos"
                                                                                                                                                                        else -> "Sin permisos de alto riesgo"
                                                                                                        }
                                                                                                                return PermissionAuditEntry(packageName = pkgInfo.packageName, appName = appName, permissions = items, riskLevel = riskLevel, reason = reason)
                    }
}
