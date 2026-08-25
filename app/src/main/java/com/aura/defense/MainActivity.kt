package com.aura.defense

import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.usage.UsageStatsManager
import android.provider.Settings
import android.net.Uri
import android.util.Log
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.aura.defense.apps.AppScanResult
import com.aura.defense.apps.AppScanner
import com.aura.defense.apps.InstalledAppInfo
import com.aura.defense.tools.LinkAnalysis
import com.aura.defense.tools.PasswordAudit
import com.aura.defense.reports.AuraReportBuilder
import com.aura.defense.data.AuraPreferences
import com.aura.defense.data.DeviceTelemetryProvider
import com.aura.defense.notifications.NotificationAlertStore
import com.aura.defense.threats.ThreatIntelligenceEngine
import com.aura.defense.threats.ThreatCategory
import com.aura.defense.ui.AuraBackground
import com.aura.defense.ui.AuraTheme
import com.aura.defense.ui.components.AuraBottomNav
import com.aura.defense.ui.components.AuraCenterDialog
import com.aura.defense.ui.components.AuraIdDialog
import com.aura.defense.ui.components.AuraTopBar
import com.aura.defense.ui.components.ModuleDialog
import com.aura.defense.ui.components.PostureSummaryDialog
import com.aura.defense.ui.components.TelemetryDialog
import com.aura.defense.ui.components.AppRisksDialog
import com.aura.defense.ui.components.LinkAnalyzerDialog
import com.aura.defense.ui.components.PasswordAuditorDialog
import com.aura.defense.ui.components.ReportDialog
import com.aura.defense.ui.components.ShareScannerDialog
import com.aura.defense.ui.components.QrScannerDialog
import com.aura.defense.ui.components.NotificationGuardDialog
import com.aura.defense.ui.components.isNotificationAccessEnabled
import com.aura.defense.security.SecurityPostureEngine
import com.aura.defense.security.SettingsAction
import com.aura.defense.ui.screens.AppsScreen
import com.aura.defense.ui.screens.AurasScreen
import com.aura.defense.ui.screens.DefenseScreen
import com.aura.defense.ui.screens.HomeScreen

class MainActivity : ComponentActivity() {
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        sharedText = extractSharedText(intent)
        val preferences = AuraPreferences(this)
        setContent {
            AuraTheme {
                AuraDefenseApp(
                    preferences = preferences,
                    telemetryProvider = DeviceTelemetryProvider(this@MainActivity),
                    sharedText = sharedText,
                    onSharedTextConsumed = { sharedText = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText = extractSharedText(intent)
    }
}

@Composable
private fun AuraDefenseApp(
    preferences: AuraPreferences,
    telemetryProvider: DeviceTelemetryProvider,
    sharedText: String?,
    onSharedTextConsumed: () -> Unit
) {
    val context = LocalContext.current
    val engine = remember { SecurityPostureEngine() }
    val appScanner = remember { AppScanner(context) }
    val threatEngine = remember { ThreatIntelligenceEngine(context) }
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf(com.aura.defense.security.PostureResult.pending()) }
    var selectedTab by remember { mutableStateOf(0) }
    var auraId by remember { mutableStateOf(preferences.getAuraId()) }
    var visibilityVisible by remember { mutableStateOf(true) }
    var showAuraCenter by remember { mutableStateOf(false) }
    var showAuraIdDialog by remember { mutableStateOf(false) }
    var showTelemetry by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }
    var moduleDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var appScanResult by remember { mutableStateOf<AppScanResult?>(null) }
    var scanningApps by remember { mutableStateOf(false) }
    var showAppRisks by remember { mutableStateOf(false) }
    var showLinkAnalyzer by remember { mutableStateOf(false) }
    var showPasswordAuditor by remember { mutableStateOf(false) }
    var showReports by remember { mutableStateOf(false) }
    var showShareScanner by remember { mutableStateOf(sharedText != null) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showNotificationGuard by remember { mutableStateOf(false) }
    var showIntro by remember { mutableStateOf(true) }
    var locationActive by remember { mutableStateOf(hasLocationPermission(context)) }
    var linkHistory by remember { mutableStateOf<List<LinkAnalysis>>(emptyList()) }
    var passwordAudit by remember { mutableStateOf<PasswordAudit?>(null) }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationActive = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!locationActive) moduleDialog = "Permiso de ubicación" to "Permiso de ubicación denegado. Aura mantendrá el modo privado."
    }

    LaunchedEffect(sharedText) {
        if (sharedText != null) showShareScanner = true
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(6000)
        showIntro = false
    }

    LaunchedEffect(Unit) {
        runCatching { engine.evaluate(telemetryProvider.read()) }
            .onSuccess { result = it }
            .onFailure {
                Log.e("AuraDefense", "No se pudo completar el diagnóstico inicial", it)
                moduleDialog = "Diagnóstico no disponible" to "No se pudo completar el diagnóstico en este dispositivo. Aura seguirá funcionando con los datos disponibles."
            }
    }

    Crossfade(targetState = showIntro, animationSpec = tween(650), label = "aura-core-transition") { showingIntro ->
        if (showingIntro) {
            com.aura.defense.ui.components.AuraCoreIntro()
        } else Scaffold(
        containerColor = AuraBackground,
        topBar = {
            androidx.compose.foundation.layout.Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                AuraTopBar(
                    auraId = auraId,
                    onAuraIdClick = { showAuraIdDialog = true },
                    onSettingsClick = { showAuraCenter = true }
                )
            }
        },
        bottomBar = { AuraBottomNav(selectedTab) { selectedTab = it } }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 18.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    result = result,
                    onStartScan = {
                        runCatching { engine.evaluate(telemetryProvider.read()) }
                            .onSuccess { result = it; showSummary = true }
                            .onFailure {
                                Log.e("AuraDefense", "No se pudo completar el diagnóstico solicitado", it)
                                moduleDialog = "Diagnóstico no disponible" to "No se pudo completar el diagnóstico en este dispositivo. Aura seguirá funcionando con los datos disponibles."
                            }
                    },
                    onModuleDialog = { title, message -> moduleDialog = title to message },
                    onEmergency = {
                        runCatching { engine.evaluate(telemetryProvider.read()) }
                            .onSuccess { result = it; showSummary = true }
                            .onFailure { moduleDialog = "Modo emergencia" to "No se pudo actualizar el diagnóstico en este dispositivo." }
                    }
                )
                1 -> AurasScreen(
                    locationActive = locationActive,
                    onActivateLocation = {
                        if (hasLocationPermission(context)) locationActive = true
                        else locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    onModuleDialog = { title, message -> moduleDialog = title to message }
                )
                2 -> DefenseScreen(
                    onModuleDialog = { title, message -> moduleDialog = title to message },
                    onEmergency = {
                        runCatching { engine.evaluate(telemetryProvider.read()) }
                            .onSuccess { result = it; showSummary = true }
                            .onFailure { moduleDialog = "Modo emergencia" to "No se pudo actualizar el diagnóstico en este dispositivo." }
                    }
                )
                else -> AppsScreen(
                    scanResult = appScanResult,
                    scanning = scanningApps,
                    onScan = {
                        if (!scanningApps) scope.launch {
                            scanningApps = true
                            val scan = withContext(Dispatchers.Default) { appScanner.scan() }
                            appScanResult = scan
                            result = engine.evaluate(result.telemetry, scan.riskyApps.size, scan.highRiskApps.size)
                            scanningApps = false
                            moduleDialog = "Escaneo de apps" to "Escaneo completado: ${scan.apps.size} apps visibles por Android. Riesgos: ${scan.riskyApps.size}. Riesgo alto: ${scan.highRiskApps.size}."
                        }
                    },
                    onViewRisks = { showAppRisks = true },
                    onExport = { showReports = true },
                    onModuleDialog = { title, message -> moduleDialog = title to message }
                )
            }
        }
        }
    }

    if (showAuraIdDialog) {
        AuraIdDialog(
            currentId = auraId,
            onSave = { value ->
                preferences.saveAuraId(value)
                auraId = value.trim()
            },
            onDismiss = { showAuraIdDialog = false }
        )
    }
    if (showAuraCenter) {
        AuraCenterDialog(
            auraId = auraId,
            visibilityVisible = visibilityVisible,
            onEditId = { showAuraIdDialog = true },
            onVisibilityToggle = { visibilityVisible = !visibilityVisible },
            onTelemetry = { showTelemetry = true },
            notificationGuardStatus = if (isNotificationAccessEnabled(context)) "Activo" else "Desactivado · Acceso requerido",
            permissionStatus = { item ->
                when (item) {
                    "Permiso de cámara" -> "Abrir escáner QR"
                    "Permiso de ubicación" -> if (hasLocationPermission(context)) "Activo" else "Acceso requerido"
                    "Permiso de notificaciones" -> "No requerido en esta versión de Android"
                    "Acceso a notificaciones" -> if (isNotificationAccessEnabled(context)) "Activo" else "Acceso requerido"
                    "Acceso al uso" -> if (hasUsageAccess(context)) "Activo" else "Acceso requerido"
                    "Optimización de batería" -> "Abrir configuración"
                    else -> "No disponible"
                }
            },
            onItemClick = { item ->
                if (item == "Escáner de apps") {
                    selectedTab = 3
                    showAuraCenter = false
                } else if (item == "Analizador de enlaces") {
                    showLinkAnalyzer = true
                    showAuraCenter = false
                } else if (item == "Auditor de contraseñas") {
                    showPasswordAuditor = true
                    showAuraCenter = false
                } else if (item == "QR Anti-Phishing") {
                    showQrScanner = true
                    showAuraCenter = false
                } else if (item == "Protección de notificaciones") {
                    showNotificationGuard = true
                    showAuraCenter = false
                } else if (item == "Inteligencia de amenazas") {
                    moduleDialog = item to "Indicadores cargados: ${threatEngine.indicators.size}. Categorías disponibles: ${threatEngine.categories.joinToString(", ") { it.toSpanish() }}. Última actualización incluida: ${threatEngine.lastUpdatedAt}. Aura usa inteligencia local incluida en la app. No se suben URLs ni datos del dispositivo en esta fase."
                    showAuraCenter = false
                } else if (item == "Permiso de cámara") {
                    showQrScanner = true
                    showAuraCenter = false
                } else if (item == "Permiso de ubicación") {
                    locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                } else if (item == "Acceso a notificaciones") {
                    showNotificationGuard = true
                    showAuraCenter = false
                } else if (item == "Acceso al uso") {
                    openSetting(Settings.ACTION_USAGE_ACCESS_SETTINGS, context) { moduleDialog = "Acceso al uso" to it }
                } else if (item == "Optimización de batería") {
                    openSetting(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, Settings.ACTION_BATTERY_SAVER_SETTINGS, context) { moduleDialog = "Optimización de batería" to it }
                } else if (item == "Permiso de notificaciones") {
                    moduleDialog = item to "No requerido en esta versión de Android. Aura no envía alertas propias."
                } else if (item == "Reportes") {
                    showReports = true
                    showAuraCenter = false
                } else if (item == "Escáner de contenido compartido") {
                    moduleDialog = item to "Para usar esta función, comparte un enlace o texto desde otra app y selecciona AURA DEFENS."
                    showAuraCenter = false
                } else {
                    val message = when (item) {
                        "Modo de visibilidad" -> "Este ajuste solo cambia la visibilidad local del Aura ID. No publica datos ni inicia conexiones."
                        "Modo local prioritario" -> "Los análisis disponibles se ejecutan localmente en este dispositivo."
                        "No subir apps", "No subir ubicación", "No subir URLs" -> "Aura no sube estos datos. El funcionamiento actual es local."
                        "Borrar historial local" -> "El historial de enlaces disponible en esta sesión se mantiene local y puede borrarse al cerrar la aplicación."
                        "Bóveda cifrada" -> "La bóveda cifrada aún no está disponible. No se guardarán datos como si estuviera activa."
                        "Comprobaciones programadas" -> "Las comprobaciones programadas aún no están disponibles. La optimización de batería solo puede abrirse desde sus ajustes de Android."
                        "Acceso rápido" -> "El acceso rápido aún no está disponible en este dispositivo."
                        "Protección de notificaciones" -> "Abre Protección de notificaciones para revisar el acceso real y las alertas locales."
                        "Auras LAN" -> "El descubrimiento LAN real se implementará con UDP. No se mostrarán Auras cercanas hasta que una instancia real responda."
                        else -> "Esta opción informativa no tiene una acción nativa disponible en Android."
                    }
                    moduleDialog = item to message
                }
            },
            onDismiss = { showAuraCenter = false }
        )
    }
    if (showTelemetry) {
        TelemetryDialog(result.telemetry, onSettingsAction = { action -> openAndroidSettings(action, context) }, onDismiss = { showTelemetry = false })
    }
    if (showAppRisks) {
        AppRisksDialog(
            apps = appScanResult?.riskyApps.orEmpty(),
            onDetails = { openAppSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, it, context) },
            onPermissions = { openAppSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, it, context) },
            onUninstall = { openAppSettings(Intent.ACTION_DELETE, it, context) },
            onDismiss = { showAppRisks = false }
        )
    }
    if (showLinkAnalyzer) {
        LinkAnalyzerDialog(
            onAnalysis = { analysis -> linkHistory = (linkHistory + analysis).takeLast(20) },
            onDismiss = { showLinkAnalyzer = false }
        )
    }
    if (showPasswordAuditor) {
        PasswordAuditorDialog(
            onAudit = { passwordAudit = it },
            onDismiss = { showPasswordAuditor = false }
        )
    }
    if (showQrScanner) {
        QrScannerDialog(
            onAnalysis = { analysis -> linkHistory = (linkHistory + analysis).takeLast(20) },
            onDismiss = { showQrScanner = false }
        )
    }
    if (showNotificationGuard) {
        NotificationGuardDialog(onDismiss = { showNotificationGuard = false })
    }
    if (showReports) {
        ReportDialog(
            onExport = { json ->
                val notificationAlerts = NotificationAlertStore(context).getAll()
                val content = if (json) AuraReportBuilder().json(auraId, result, appScanResult, linkHistory, passwordAudit, notificationAlerts, threatEngine.indicators)
                else AuraReportBuilder().text(auraId, result, appScanResult, linkHistory, passwordAudit, notificationAlerts, threatEngine.indicators)
                shareReport(content, json, context)
                showReports = false
            },
            onDismiss = { showReports = false }
        )
    }
    if (showShareScanner && sharedText != null) {
        ShareScannerDialog(
            text = sharedText,
            onAnalyses = { analyses ->
                linkHistory = (linkHistory + analyses).takeLast(20)
            },
            onDismiss = {
                showShareScanner = false
                onSharedTextConsumed()
            }
        )
    }
    if (showSummary) {
        PostureSummaryDialog(
            result,
            onFindingAction = { finding ->
                finding.settingsAction?.let { openAndroidSettings(it, context) }
                    ?: run { moduleDialog = finding.title to finding.recommendedAction }
            },
            onDismiss = { showSummary = false }
        )
    }
    moduleDialog?.let { (title, message) -> ModuleDialog(title, message) { moduleDialog = null } }
}

private fun extractSharedText(intent: Intent?): String? = runCatching {
    if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
    intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
}.onFailure { Log.e("AuraDefense", "No se pudo leer el contenido compartido", it) }.getOrNull()

private fun ThreatCategory.toSpanish() = when (this) {
    ThreatCategory.PHISHING -> "suplantación"
    ThreatCategory.MALWARE -> "software malicioso"
    ThreatCategory.SPYWARE -> "vigilancia"
    ThreatCategory.BOTNET -> "botnet"
    ThreatCategory.C2 -> "control remoto"
    ThreatCategory.TRACKING -> "seguimiento"
    ThreatCategory.ADS -> "publicidad"
    ThreatCategory.CRYPTO_SCAM -> "estafa de activos digitales"
}

private fun hasLocationPermission(context: Context): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun hasUsageAccess(context: Context): Boolean = runCatching {
    val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, System.currentTimeMillis() - 60_000L, System.currentTimeMillis()).isNotEmpty()
}.getOrDefault(false)

private fun openSetting(action: String, context: Context, onFailure: (String) -> Unit) {
    openSetting(action, null, context, onFailure)
}

private fun openSetting(action: String, fallbackAction: String?, context: Context, onFailure: (String) -> Unit) {
    runCatching { context.startActivity(Intent(action)) }
        .onFailure {
            if (fallbackAction == null) onFailure("No se pudo abrir esta configuración en este dispositivo.")
            else runCatching { context.startActivity(Intent(fallbackAction)) }
                .onFailure { onFailure("No se pudo abrir esta configuración en este dispositivo.") }
        }
}

private fun openAndroidSettings(action: SettingsAction, context: Context) {
    val intentAction = when (action) {
        SettingsAction.VPN -> Settings.ACTION_VPN_SETTINGS
        SettingsAction.SEGURIDAD -> Settings.ACTION_SECURITY_SETTINGS
        SettingsAction.DESARROLLADOR -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
        SettingsAction.RED -> Settings.ACTION_WIRELESS_SETTINGS
        SettingsAction.NOTIFICACIONES -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
    }
    runCatching { context.startActivity(Intent(intentAction)) }
        .onFailure { Log.e("AuraDefense", "No se pudo abrir el ajuste de Android", it) }
}

private fun openAppSettings(action: String, app: InstalledAppInfo, context: Context) {
    runCatching {
        context.startActivity(Intent(action, Uri.parse("package:${app.packageName}")))
    }.onFailure { Log.e("AuraDefense", "No se pudo abrir el ajuste de la app", it) }
}

private fun shareReport(content: String, json: Boolean, context: Context) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (json) "application/json" else "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, if (json) "Informe Aura JSON" else "Informe Aura")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir informe"))
    }.onFailure { Log.e("AuraDefense", "No se pudo compartir el informe", it) }
}
