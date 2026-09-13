package com.aura.defense.monitor

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.aura.defense.apps.AppScanResult
import com.aura.defense.vpn.DnsFirewallStore

data class CorrelationAlert(
    val severity: String,
    val title: String,
    val detail: String,
    val evidence: List<String>
)

object AuraCorrelationEngine {
    fun deviceAdminPackages(context: Context): List<String> = runCatching {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        dpm?.activeAdmins?.map { it.packageName }.orEmpty()
    }.getOrDefault(emptyList())

    fun foreignNotificationListeners(context: Context): List<String> = runCatching {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_NOTIFICATION_LISTENERS
        ).orEmpty()
        enabled.split(':')
            .mapNotNull { component -> component.substringBefore('/').takeIf { it.isNotBlank() } }
            .filter { it != context.packageName }
            .distinct()
    }.getOrDefault(emptyList())

    fun defaultRoleHolders(context: Context): Map<String, String> = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@runCatching emptyMap()
        val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
        buildMap {
            listOf(
                android.app.role.RoleManager.ROLE_SMS,
                android.app.role.RoleManager.ROLE_BROWSER
            ).forEach { role ->
                roleManager?.getRoleHolders(role)?.firstOrNull()?.let { put(role, it) }
            }
        }
    }.getOrDefault(emptyMap())

    fun detectBankingTrojanPattern(
        context: Context,
        scan: AppScanResult?
    ): List<CorrelationAlert> {
        val now = System.currentTimeMillis()
        val weekMs = 7L * 24 * 60 * 60 * 1000
        return scan?.riskyApps.orEmpty().mapNotNull { app ->
            val recentInstall = now - app.firstInstallTimeAsLong() in 1..weekMs
            val outsideStore = app.installerPackage.isBlank() || app.installerPackage == "Desconocido" ||
                (!app.installerPackage.contains("vending", ignoreCase = true) &&
                    !app.installerPackage.contains("packageinstaller", ignoreCase = true) &&
                    !app.installerPackage.contains("android", ignoreCase = true))
            val smsSignal = app.findings.any { it.reason.contains("SMS", ignoreCase = true) }
            val overlaySignal = app.findings.any { it.reason.contains("superposición", ignoreCase = true) }
            if (!recentInstall || !outsideStore || (!smsSignal && !overlaySignal)) return@mapNotNull null

            CorrelationAlert(
                severity = "CRITICAL",
                title = "Patrón de troyano bancario: ${app.appName}",
                detail = "Instalada hace menos de 7 días fuera de tienda, con señales SMS o superposición usadas por troyanos bancarios.",
                evidence = listOf(
                    "APP: ${app.appName} (${app.packageName})",
                    "ESCÁNER: ${app.findings.joinToString("; ") { it.reason }}",
                    "INSTALADOR: ${app.installerPackage}",
                    "INSTALACIÓN: hace menos de 7 días"
                )
            )
        }
    }

    fun detectStalkerwarePattern(
        context: Context,
        scan: AppScanResult?
    ): List<CorrelationAlert> {
        val knownSystem = listOf("com.android.", "com.google.android.", "com.samsung.", "com.miui.", "com.xiaomi.", "com.huawei.")
        val suspiciousAdmins = deviceAdminPackages(context).filter { admin ->
            knownSystem.none { admin.startsWith(it) } &&
                scan?.apps?.any { it.packageName == admin && it.findings.isNotEmpty() } == true
        }
        val listeners = foreignNotificationListeners(context)
        if (suspiciousAdmins.isEmpty()) return emptyList()

        return listOf(
            CorrelationAlert(
                severity = if (listeners.isNotEmpty()) "CRITICAL" else "HIGH",
                title = "Patrón de stalkerware: administrador no reconocido",
                detail = "Una app de terceros tiene control administrativo del dispositivo" +
                    if (listeners.isNotEmpty()) " y otra app puede leer notificaciones." else ".",
                evidence = buildList {
                    add("ADMIN DEVICE: ${suspiciousAdmins.joinToString(", ")}")
                    if (listeners.isNotEmpty()) add("LISTENER NOTIFICACIONES: ${listeners.joinToString(", ")}")
                    scan?.apps?.filter { it.packageName in suspiciousAdmins }?.forEach { app ->
                        if (app.findings.isNotEmpty()) add("ESCÁNER ${app.packageName}: ${app.findings.joinToString("; ") { it.reason }}")
                    }
                }
            )
        )
    }

    fun detectDnsFromFreshInstall(
        context: Context,
        scan: AppScanResult?
    ): List<CorrelationAlert> {
        val recent = DnsFirewallStore(context).blockedEvents()
            .filter { System.currentTimeMillis() - it.timestamp in 0..(10 * 60 * 1000L) }
        if (recent.size < 3) return emptyList()

        val freshApps = scan?.apps.orEmpty().filter {
            System.currentTimeMillis() - it.firstInstallTimeAsLong() in 1..(7 * 24 * 60 * 60 * 1000L)
        }
        if (freshApps.isEmpty()) return emptyList()
        return listOf(
            CorrelationAlert(
                severity = "HIGH",
                title = "Actividad DNS maliciosa concentrada",
                detail = "El cortafuegos bloqueó ${recent.size} dominios peligrosos en los últimos 10 minutos${if (freshApps.isNotEmpty()) " mientras hay apps instaladas recientemente" else ""}.",
                evidence = buildList {
                    addAll(recent.take(5).map { "DNS: ${it.domain} [${it.category}/${it.severity}]" })
                    freshApps.forEach { add("APP RECIENTE: ${it.appName} (${it.packageName})") }
                }
            )
        )
    }

    fun evaluate(context: Context, scan: AppScanResult?): List<CorrelationAlert> =
        detectBankingTrojanPattern(context, scan) +
            detectStalkerwarePattern(context, scan) +
            detectDnsFromFreshInstall(context, scan)
}
