package com.aura.defense.apps

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppRiskSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class AppRiskFinding(val severity: AppRiskSeverity, val reason: String)

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val installerPackage: String,
    val firstInstallTime: String,
    val lastUpdateTime: String,
    val targetSdk: Int,
    val requestedPermissions: List<String>,
    val grantedDangerousPermissions: List<String>,
    val isSystemApp: Boolean,
    val isDebuggable: Boolean,
    val allowBackup: Boolean,
    val requestsInstallPackages: Boolean,
    val findings: List<AppRiskFinding>
)

data class AppScanResult(
    val apps: List<InstalledAppInfo>,
    val scannedAt: String,
    val failed: Boolean = false
) {
    val riskyApps: List<InstalledAppInfo> get() = apps.filter { it.findings.isNotEmpty() }
    val highRiskApps: List<InstalledAppInfo> get() = apps.filter { app -> app.findings.any { it.severity >= AppRiskSeverity.HIGH } }
}

class AppScanner(private val context: Context) {
    fun scan(): AppScanResult {
        val now = timestamp(System.currentTimeMillis())
        return runCatching {
            val packageManager = context.packageManager
            val applications = runCatching {
                packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
            }.getOrElse {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }
            val apps = applications.mapNotNull { application -> readApp(packageManager, application) }
            AppScanResult(apps = apps, scannedAt = now)
        }.onFailure { Log.e("AuraDefense", "No se pudo completar el escaneo de apps", it) }
            .getOrElse { AppScanResult(emptyList(), now, failed = true) }
    }

    private fun readApp(packageManager: PackageManager, application: ApplicationInfo): InstalledAppInfo? = runCatching {
        val packageInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(application.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(application.packageName, PackageManager.GET_PERMISSIONS)
            }
        }.getOrElse { return null }
        val requested = packageInfo.requestedPermissions?.toList().orEmpty()
        val granted = requested.filterIndexed { index, permission ->
            packageInfo.requestedPermissionsFlags?.getOrNull(index)?.and(PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0 && isDangerous(permission)
        }
        val system = application.flags and ApplicationInfo.FLAG_SYSTEM != 0
        val debuggable = application.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val allowBackup = application.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0
        val findings = buildFindings(application, packageInfo, requested, system, debuggable, allowBackup)
        InstalledAppInfo(
            appName = runCatching { packageManager.getApplicationLabel(application).toString() }.getOrDefault("Aplicación sin nombre"),
            packageName = application.packageName,
            versionName = packageInfo.versionName ?: "No disponible",
            versionCode = readVersionCode(packageInfo),
            installerPackage = runCatching { packageManager.getInstallerPackageName(application.packageName) ?: "Desconocido" }.getOrDefault("Desconocido"),
            firstInstallTime = timestamp(packageInfo.firstInstallTime),
            lastUpdateTime = timestamp(packageInfo.lastUpdateTime),
            targetSdk = application.targetSdkVersion,
            requestedPermissions = requested,
            grantedDangerousPermissions = granted,
            isSystemApp = system,
            isDebuggable = debuggable,
            allowBackup = allowBackup,
            requestsInstallPackages = requested.contains("android.permission.REQUEST_INSTALL_PACKAGES"),
            findings = findings
        )
    }.onFailure { Log.e("AuraDefense", "No se pudo analizar una app visible", it) }.getOrNull()

    private fun buildFindings(application: ApplicationInfo, info: PackageInfo, permissions: List<String>, system: Boolean, debuggable: Boolean, allowBackup: Boolean): List<AppRiskFinding> = buildList {
        if (debuggable) add(AppRiskFinding(AppRiskSeverity.MEDIUM, "Aplicación depurable"))
        if (application.targetSdkVersion in 1..28) add(AppRiskFinding(AppRiskSeverity.MEDIUM, "SDK antiguo"))
        if (permissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")) add(AppRiskFinding(AppRiskSeverity.HIGH, "Puede instalar APKs"))
        if (permissions.any { it.startsWith("android.permission.READ_SMS") || it.startsWith("android.permission.SEND_SMS") || it.startsWith("android.permission.RECEIVE_SMS") }) add(AppRiskFinding(AppRiskSeverity.HIGH, "Permiso sensible detectado: SMS"))
        if (permissions.any { it.startsWith("android.permission.READ_CONTACTS") || it.startsWith("android.permission.WRITE_CONTACTS") }) add(AppRiskFinding(AppRiskSeverity.MEDIUM, "Permiso sensible detectado: contactos"))
        if (permissions.any { it == Manifest.permission.ACCESS_FINE_LOCATION || it == Manifest.permission.ACCESS_COARSE_LOCATION }) add(AppRiskFinding(AppRiskSeverity.LOW, "Permiso sensible detectado: ubicación"))
        if (permissions.contains(Manifest.permission.CAMERA)) add(AppRiskFinding(AppRiskSeverity.LOW, "Permiso sensible detectado: cámara"))
        if (permissions.contains(Manifest.permission.RECORD_AUDIO)) add(AppRiskFinding(AppRiskSeverity.MEDIUM, "Permiso sensible detectado: micrófono"))
        if (permissions.any { it.startsWith("android.permission.READ_PHONE") || it.startsWith("android.permission.CALL_PHONE") }) add(AppRiskFinding(AppRiskSeverity.MEDIUM, "Permiso sensible detectado: teléfono"))
        if (permissions.contains("android.permission.SYSTEM_ALERT_WINDOW")) add(AppRiskFinding(AppRiskSeverity.HIGH, "Permiso sensible detectado: superposición"))
        if (!system && allowBackup) add(AppRiskFinding(AppRiskSeverity.LOW, "Copia de seguridad permitida"))
        val installer = runCatching { context.packageManager.getInstallerPackageName(info.packageName) }.getOrNull()
        if (!system && installer.isNullOrBlank()) add(AppRiskFinding(AppRiskSeverity.LOW, "Instalador desconocido"))
    }

    private fun isDangerous(permission: String): Boolean = permission in setOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE)

    private fun readVersionCode(info: PackageInfo): Long = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }.getOrDefault(0L)

    private fun timestamp(value: Long): String = runCatching { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(value)) }.getOrDefault("No disponible")
}
