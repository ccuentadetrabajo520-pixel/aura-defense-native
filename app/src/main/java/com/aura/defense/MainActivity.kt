package com.aura.defense

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    var linkHistory by remember { mutableStateOf<List<LinkAnalysis>>(emptyList()) }
    var passwordAudit by remember { mutableStateOf<PasswordAudit?>(null) }

    LaunchedEffect(sharedText) {
        if (sharedText != null) showShareScanner = true
    }

    LaunchedEffect(Unit) {
        runCatching { engine.evaluate(telemetryProvider.read()) }
            .onSuccess { result = it }
            .onFailure {
                Log.e("AuraDefense", "No se pudo completar el diagnóstico inicial", it)
                moduleDialog = "Diagnóstico no disponible" to "No se pudo completar el diagnóstico en este dispositivo. Aura seguirá funcionando con los datos disponibles."
            }
    }

    Scaffold(
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
                    onModuleDialog = { title, message -> moduleDialog = title to message }
                )
                1 -> AurasScreen { title, message -> moduleDialog = title to message }
                2 -> DefenseScreen { title, message -> moduleDialog = title to message }
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
                } else if (item == "Reportes") {
                    showReports = true
                    showAuraCenter = false
                } else if (item == "Escáner de contenido compartido") {
                    moduleDialog = item to "Para usar esta función, comparte un enlace o texto desde otra app y selecciona Aura Defense."
                    showAuraCenter = false
                } else {
                    moduleDialog = item to "Este módulo se conectará a la API real de Android en la siguiente fase. No se mostrará como activo hasta que funcione de verdad."
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
                val content = if (json) AuraReportBuilder().json(auraId, result, appScanResult, linkHistory, passwordAudit, notificationAlerts)
                else AuraReportBuilder().text(auraId, result, appScanResult, linkHistory, passwordAudit, notificationAlerts)
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
