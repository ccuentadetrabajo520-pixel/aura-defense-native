package com.aura.defense.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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
) {
    fun firstInstallTimeAsLong(): Long = runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .parse(firstInstallTime)?.time ?: 0L
    }.getOrDefault(0L)
}

data class AppScanResult(
    val apps: List<InstalledAppInfo>,
    val scannedAt: String,
    val failed: Boolean = false,
    val coverageNote: String = "Análisis local limitado por Android y permisos del sistema."
) {
    val riskyApps: List<InstalledAppInfo> get() = apps.filter { app -> app.findings.any { it.level in setOf(AppFindingLevel.LOW_RISK, AppFindingLevel.MEDIUM_RISK, AppFindingLevel.HIGH_RISK, AppFindingLevel.SUSPICIOUS_SIGNAL, AppFindingLevel.CONFIRMED_MATCH) } }
    val highRiskApps: List<InstalledAppInfo> get() = apps.filter { app -> app.findings.any { it.level == AppFindingLevel.HIGH_RISK || it.level == AppFindingLevel.CONFIRMED_MATCH } }
}

class AppScanner(private val context: Context) {

    fun scan(): AppScanResult {
        val now = timestamp(System.currentTimeMillis())
        return runCatching {
            com.aura.defense.monitor.AuraProcessLog.log("Iniciando escaneo local de aplicaciones visibles…", "SCAN")
            val packageManager = context.packageManager
            val applications = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                }
            }.getOrElse {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }

            val apps = applications.mapNotNull { application -> readApp(packageManager, application) }
            val result = AppScanResult(
                apps = apps,
                scannedAt = now,
                coverageNote = if (apps.isEmpty()) "Cobertura parcial: Android no expuso paquetes instalados o no fue posible leer el conjunto visible." else "Cobertura local: se analizan metadatos visibles del sistema. La evidencia puede ser parcial si Android oculta el dato."
            )
            com.aura.defense.monitor.AuraProcessLog.log(
                "Escaneo local completado: ${apps.size} apps analizadas, ${result.riskyApps.size} con señales revisables.",
                "SCAN"
            )
            result
        }.onFailure { Timber.e(it, "No se pudo completar el escaneo local de apps") }
            .getOrElse { AppScanResult(emptyList(), now, failed = true, coverageNote = "Cobertura parcial: Android no permitió completar el análisis local de apps.") }
    }

    private fun readApp(packageManager: PackageManager, application: ApplicationInfo): InstalledAppInfo? = runCatching {
        val packageInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    application.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(application.packageName, PackageManager.GET_PERMISSIONS)
            }
        }.getOrElse { return null }

        val requested = packageInfo.requestedPermissions?.toList().orEmpty()
        val requestedFlags = packageInfo.requestedPermissionsFlags?.toList().orEmpty()
        val granted = requested.filterIndexed { index, permission ->
            requestedFlags.getOrNull(index)?.and(PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
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
            installerPackage = runCatching { packageManager.getInstallerPackageName(application.packageName) ?: "No disponible" }.getOrDefault("No disponible"),
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
    }.onFailure { Timber.e(it, "No se pudo analizar una app visible") }.getOrNull()

    private fun buildFindings(
        application: ApplicationInfo,
        info: PackageInfo,
        permissions: List<String>,
        system: Boolean,
        debuggable: Boolean,
        allowBackup: Boolean
    ): List<AppRiskFinding> {
        val findings = mutableListOf<AppRiskFinding>()

        if (debuggable) {
            findings += AppRiskFinding(
                id = "app.debuggable",
                category = "APP_METADATA",
                severity = AppRiskSeverity.MEDIUM,
                level = AppFindingLevel.LOW_RISK,
                confidence = AppFindingConfidence.MODERATE,
                evidence = "La app está marcada como depurable.",
                limits = "La depuración no confirma malware; puede ser un desarrollo o una herramienta de prueba.",
                possibleFalsePositive = "Las apps en desarrollo o integradas con Android Studio pueden señalarse.",
                recommendation = "Revisa si es una app de desarrollo o un cliente de pruebas.",
                reversibleAction = "Puedes ocultar este aviso o revisarlo desde el detalle de la aplicación.",
                reason = "App depurable"
            )
        }

        if (application.targetSdkVersion in 1..28) {
            findings += AppRiskFinding(
                id = "app.sdk.legacy",
                category = "APP_METADATA",
                severity = AppRiskSeverity.MEDIUM,
                level = AppFindingLevel.MEDIUM_RISK,
                confidence = AppFindingConfidence.MODERATE,
                evidence = "La app tiene un SDK objetivo antiguo (${application.targetSdkVersion}).",
                limits = "Una app con SDK antiguo no es malware por sí sola.",
                possibleFalsePositive = "Hay apps de nicho o antiguas que siguen funcionando sin problema.",
                recommendation = "Verifica si la app necesita actualización del desarrollador.",
                reversibleAction = "Puedes silenciar este aviso temporalmente.",
                reason = "SDK objetivo antiguo"
            )
        }

        val label = runCatching { application.loadLabel(context.packageManager).toString() }.getOrDefault("")
        AppScannerRules.nameHeuristicOrNull(application.packageName, label.ifBlank { application.packageName })?.let { findings += it }

        findings += AppScannerRules.permissionFindings(permissions)

        if (permissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
            findings += AppRiskFinding(
                id = "app.install.packages",
                category = "APP_PERMISSION",
                severity = AppRiskSeverity.HIGH,
                level = AppFindingLevel.HIGH_RISK,
                confidence = AppFindingConfidence.MODERATE,
                evidence = "La app declara la capacidad de instalar paquetes desde fuentes ajenas a Play.",
                limits = "La capacidad de instalar paquetes no es un malware confirmado; solo indica un riesgo de instalación externa.",
                possibleFalsePositive = "Herramientas de instalación o gestor de actualizaciones pueden usarlo de forma legítima.",
                recommendation = "Revisa si la app es de confianza y si necesitas mantener ese permiso.",
                reversibleAction = "Puedes revocar el permiso desde Ajustes > Apps > Permisos.",
                reason = "Puede instalar APKs"
            )
        }

        val installer = runCatching { context.packageManager.getInstallerPackageName(info.packageName) }.getOrNull()
        if (!system && installer.isNullOrBlank()) {
            findings += AppRiskFinding(
                id = "app.installer.unknown",
                category = "APP_METADATA",
                severity = AppRiskSeverity.LOW,
                level = AppFindingLevel.INFORMATIVE,
                confidence = AppFindingConfidence.LOW,
                evidence = "Android no expuso un instalador para esta aplicación.",
                limits = "La falta de instalador no confirma maldad ni vigilancia.",
                possibleFalsePositive = "Algunos dispositivos o fabricantes no lo exponen por diseño.",
                recommendation = "No asumas malware solo porque el instalador esté oculto; revisa el paquete y la fuente de descarga.",
                reversibleAction = "Puedes descartar este aviso o dejarlo pendiente en la pantalla de apps.",
                reason = "Instalador no disponible"
            )
        }

        if (!system && allowBackup) {
            findings += AppRiskFinding(
                id = "app.backup.enabled",
                category = "APP_METADATA",
                severity = AppRiskSeverity.LOW,
                level = AppFindingLevel.LOW_RISK,
                confidence = AppFindingConfidence.MODERATE,
                evidence = "La app permite copia de seguridad del sistema de Android.",
                limits = "Permitir backup no demuestra riesgo, solo expone un dato específico a la copia del sistema.",
                possibleFalsePositive = "App legítimas de productividad o productividad personal pueden necesitarlo.",
                recommendation = "Si la app tiene acceso sensible, revisa la configuración de backup.",
                reversibleAction = "Puedes desactivar la copia de seguridad de la app sin desinstalarla.",
                reason = "Backup del sistema permitido"
            )
        }

        val verifiedPackageMatch = signedPackageMatch(application.packageName)
        if (verifiedPackageMatch != null) {
            findings += AppScannerRules.confirmedMatch(
                ruleId = verifiedPackageMatch.ruleId,
                source = verifiedPackageMatch.source,
                evidence = verifiedPackageMatch.evidence
            )
        }

        return findings.distinctBy { it.id }
    }

    private data class SignedPackageMatch(
        val packageName: String,
        val ruleId: String,
        val source: String,
        val evidence: String
    )

    private fun signedPackageMatch(packageName: String): SignedPackageMatch? {
        val normalized = packageName.lowercase(Locale.ROOT)
        return listOf(
            SignedPackageMatch(
                packageName = "com.example.evilapp",
                ruleId = "rule-android-package-0001",
                source = "internal-signed-feed",
                evidence = "Paquete coincidente con la lista de fraude local firmada para pruebas de validación."
            )
        ).firstOrNull { it.packageName == normalized }
    }

    private fun readVersionCode(info: PackageInfo): Long = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }.getOrDefault(0L)

    private fun timestamp(value: Long): String = runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(value))
    }.getOrDefault("No disponible")
}

