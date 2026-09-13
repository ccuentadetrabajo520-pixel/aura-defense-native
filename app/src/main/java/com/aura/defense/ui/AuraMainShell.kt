package com.aura.defense.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.aura.defense.MainActivity
import com.aura.defense.apps.AppScanResult
import com.aura.defense.apps.AppScanner
import com.aura.defense.guardian.AuraGuardianEngine
import com.aura.defense.history.AuraHistoryEntry
import com.aura.defense.reports.AuraReportBuilder
import com.aura.defense.security.PostureResult
import com.aura.defense.tools.LinkAnalysis
import com.aura.defense.ui.components.AppRisksDialog
import com.aura.defense.ui.components.AuraGuardianDialog
import com.aura.defense.ui.components.AuraHistoryDialog
import com.aura.defense.ui.components.AuraToolsHubDialog
import com.aura.defense.ui.components.AuraVaultDialog
import com.aura.defense.ui.components.EmergencyModeDialog
import com.aura.defense.ui.components.EmergencyModeResult
import com.aura.defense.ui.components.LinkAnalyzerDialog
import com.aura.defense.ui.components.ModuleDialog
import com.aura.defense.ui.components.NotificationGuardDialog
import com.aura.defense.ui.components.QrScannerDialog
import com.aura.defense.monitor.AuraCorrelationEngine
import com.aura.defense.monitor.CorrelationAlert
import com.aura.defense.monitor.AuraProcessLog
import com.aura.defense.ui.screens.AuraConsoleScreen
import com.aura.defense.ui.screens.AppsScreen
import com.aura.defense.ui.screens.AurasScreen
import com.aura.defense.ui.screens.DefenseScreen
import com.aura.defense.ui.screens.HomeScreen
import com.aura.defense.ui.components.aura.AuraAvatarMini
import com.aura.defense.vpn.DnsFirewallStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val emergencySteps = listOf(
    "Telemetría del dispositivo",
    "Escaneo de aplicaciones",
    "Correlación de señales",
    "Informe local"
)

suspend fun runEmergencyScan(
    context: Context,
    appScanner: AppScanner
): Triple<PostureResult, AppScanResult, List<CorrelationAlert>> = withContext(Dispatchers.Default) {
    val telemetry = com.aura.defense.data.DeviceTelemetryProvider(context).read()
    val initialPosture = com.aura.defense.security.SecurityPostureEngine().evaluate(telemetry)
    val scan = appScanner.scan()
    val posture = com.aura.defense.security.SecurityPostureEngine().evaluate(
        telemetry,
        scan.riskyApps.size,
        scan.highRiskApps.size
    )
    val alerts = AuraCorrelationEngine.evaluate(context, scan)
    Triple(posture, scan, alerts)
}

@Composable
fun AuraMainShell(
    boot: AuraBootstrap,
    scanningApps: Boolean,
    appScanResult: AppScanResult?,
    isVpnRunning: Boolean,
    onScan: () -> Unit,
    onVpnToggle: () -> Unit,
    onProfileChange: (com.aura.defense.vpn.DnsFirewallProfile) -> Unit,
    sharedText: String?,
    onSharedTextConsumed: () -> Unit,
    sharedFile: Uri?,
    onSharedFileConsumed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appScanner = remember { AppScanner(context) }
    val processEntries by AuraProcessLog.entries.collectAsState()
    var tabIndex by remember { mutableStateOf(0) }
    var moduleDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showGuardian by remember { mutableStateOf(false) }
    var showEmergency by remember { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var showNotificationGuard by remember { mutableStateOf(false) }
    var showRisks by remember { mutableStateOf(false) }
    var showLinkAnalyzer by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showVault by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var emergencyRunning by remember { mutableStateOf(false) }
    var emergencyStep by remember { mutableStateOf(0) }
    var emergencyResult by remember { mutableStateOf<EmergencyModeResult?>(null) }
    var emergencyPosture by remember { mutableStateOf(boot.posture) }
    var emergencyScan by remember { mutableStateOf<AppScanResult?>(appScanResult) }
    var emergencyAlerts by remember { mutableStateOf<List<CorrelationAlert>>(emptyList()) }
    var historyEntries by remember { mutableStateOf<List<AuraHistoryEntry>>(boot.historyEntries) }

    val guardianEngine = remember(boot.threatEngine) { AuraGuardianEngine(boot.threatEngine) }
    val guardianAssessment = guardianEngine.assess(
        boot.posture,
        appScanResult,
        emptyList(),
        boot.notificationAlerts,
        boot.historyEntries,
        boot.blockedDns
    )
    val dialogLambda: (String, String) -> Unit = { title, message -> moduleDialog = title to message }
    val labels = listOf("Consola", "Inicio", "Defensa", "Apps", "Auras")
    val icons = listOf(Icons.Default.Home, Icons.Default.Home, Icons.Default.Security, Icons.Default.Apps, Icons.Default.Public)

    LaunchedEffect(tabIndex) {
        if (tabIndex == 0) AuraProcessLog.log("Consola iniciada", "SISTEMA")
    }

    fun startEmergency() {
        if (emergencyRunning) return
        showEmergency = true
        emergencyRunning = true
        emergencyStep = 0
        emergencyResult = null
        scope.launch {
            emergencyStep = 1
            val (posture, scan, alerts) = runEmergencyScan(context, appScanner)
            emergencyPosture = posture
            emergencyScan = scan
            emergencyAlerts = alerts
            emergencyStep = 2
            val guardian = guardianEngine.assess(
                posture,
                scan,
                emptyList(),
                boot.notificationAlerts,
                boot.historyEntries,
                boot.blockedDns
            )
            val reportText = buildString {
                appendLine("AURA DEFENS - MODO EMERGENCIA")
                appendLine("Postura: ${posture.status} (${posture.score})")
                appendLine("Apps visibles: ${scan.apps.size}")
                appendLine("Apps con riesgo: ${scan.riskyApps.size}")
                appendLine("Alertas correlacionadas: ${alerts.size}")
                appendLine("Guardián: ${guardian.level}")
            }
            val reportJson = AuraReportBuilder().json(
                auraId = boot.auraId,
                posture = posture,
                apps = scan,
                links = emptyList(),
                password = null,
                notifications = boot.notificationAlerts,
                guardian = guardian,
                history = boot.historyEntries
            )
            val reportSaved = withContext(Dispatchers.IO) {
                runCatching {
                    File(context.filesDir, "aura_emergencia.txt").writeText(reportText)
                    File(context.filesDir, "aura_emergencia.json").writeText(reportJson)
                    true
                }.getOrDefault(false)
            }
            emergencyStep = 3
            emergencyResult = EmergencyModeResult(
                posture = posture,
                appScan = scan,
                guardian = guardian,
                recentLinkCount = boot.notificationAlerts.size,
                vpnStatus = if (isVpnRunning) "Protegido" else "VPN desactivada",
                reportText = reportText,
                reportJson = reportJson,
                reportSaved = reportSaved
            )
            emergencyRunning = false
        }
    }

    fun openAppSettings(packageName: String) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        )
    }

    fun requestUninstall(packageName: String) {
        context.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AURA DEFENS", color = AuraCyan)
                Text(boot.auraId, modifier = Modifier.padding(start = 10.dp).weight(1f), color = AuraMuted)
                AuraAvatarMini(onClick = { tabIndex = 0 })
                IconButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        MainActivity.sharedDiagTree.flushNow()
                        val logFile = File(context.filesDir, "aura_diagnostico.log")
                        if (logFile.exists()) {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", logFile)
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }, "Compartir diagnóstico"))
                            }
                        }
                    }
                }) {
                    Icon(Icons.Default.BugReport, contentDescription = "Compartir diagnóstico")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        icon = { Icon(icons[index], contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            when (tabIndex) {
                0 -> AuraConsoleScreen(
                    posture = boot.posture,
                    guardianSerious = guardianAssessment.level.name in setOf("RIESGO_ALTO", "CRITICO") || appScanResult?.highRiskApps?.isNotEmpty() == true,
                    scanning = scanningApps,
                    vpnRunning = isVpnRunning,
                    onScan = onScan,
                    onVpnToggle = onVpnToggle,
                    onProfileChange = onProfileChange,
                    onModuleDialog = dialogLambda
                )
                1 -> HomeScreen(
                    result = emergencyPosture,
                    guardianAssessment = guardianAssessment,
                    correlationAlerts = emergencyAlerts,
                    historyCount = historyEntries.size,
                    onGuardianAnalysis = { showGuardian = true },
                    onStartScan = onScan,
                    onModuleDialog = dialogLambda,
                    onEmergency = ::startEmergency,
                    onToolsHub = { showTools = true }
                )
                2 -> DefenseScreen(
                    vpnStatus = if (isVpnRunning) "Protegido" else "VPN desactivada",
                    vpnRunning = isVpnRunning,
                    firewallProfile = boot.dnsProfile,
                    blockedDomains = boot.blockedDns,
                    blockedDomainCount = boot.blockedDnsCount,
                    allowlistedDomains = boot.allowlistedDomains,
                    blockedManuallyDomains = boot.blockedManuallyDomains,
                    blockPulse = processEntries.count { it.category == "RED" },
                    onProfileChange = onProfileChange,
                    onAllowlistAdd = { domain -> scope.launch(Dispatchers.IO) { DnsFirewallStore(context).addAllowlistedDomain(domain) } },
                    onAllowlistRemove = { domain -> scope.launch(Dispatchers.IO) { DnsFirewallStore(context).removeAllowlistedDomain(domain) } },
                    onBlocklistAdd = { domain -> scope.launch(Dispatchers.IO) { DnsFirewallStore(context).addBlockedDomain(domain) } },
                    onBlocklistRemove = { domain -> scope.launch(Dispatchers.IO) { DnsFirewallStore(context).removeBlockedDomain(domain) } },
                    onVpnToggle = onVpnToggle,
                    onModuleDialog = dialogLambda,
                    onEmergency = ::startEmergency
                )
                3 -> AppsScreen(
                    scanResult = appScanResult,
                    scanning = scanningApps,
                    onScan = onScan,
                    onViewRisks = { showRisks = true },
                    onExport = { moduleDialog = "Exportación" to "Disponible próximamente en esta versión." },
                    onModuleDialog = dialogLambda
                )
                4 -> AurasScreen(
                    locationActive = false,
                    lanSearching = false,
                    lanPeers = emptyList(),
                    lastLanScan = null,
                    historyEntries = historyEntries,
                    visible = true,
                    onActivateLocation = {},
                    onVisibilityToggle = {},
                    onSearchLan = {},
                    onStopLanSearch = {},
                    onModuleDialog = dialogLambda
                )
            }
        }
    }

    if (sharedText != null) {
        TextButton(onClick = onSharedTextConsumed) { Text("Contenido compartido recibido") }
    }
    if (sharedFile != null) {
        TextButton(onClick = onSharedFileConsumed) { Text("Archivo compartido recibido") }
    }

    if (showGuardian) AuraGuardianDialog(guardianAssessment) { showGuardian = false }
    if (showNotificationGuard) NotificationGuardDialog { showNotificationGuard = false }
    if (showTools) {
        AuraToolsHubDialog(onDismiss = { showTools = false })
    }
    if (showRisks) {
        AppRisksDialog(
            apps = appScanResult?.riskyApps.orEmpty(),
            onDetails = { app -> moduleDialog = app.appName to app.findings.joinToString("\n") { it.reason } },
            onPermissions = { app -> openAppSettings(app.packageName) },
            onUninstall = { app -> requestUninstall(app.packageName) },
            onDismiss = { showRisks = false }
        )
    }
    if (showLinkAnalyzer) {
        LinkAnalyzerDialog(
            onAnalysis = { analysis -> moduleDialog = "Análisis de enlace" to linkAnalysisText(analysis) },
            onDismiss = { showLinkAnalyzer = false }
        )
    }
    if (showQrScanner) {
        QrScannerDialog(
            onAnalysis = { analysis -> moduleDialog = "Análisis QR" to linkAnalysisText(analysis) },
            onDismiss = { showQrScanner = false }
        )
    }
    if (showVault) AuraVaultDialog(context) { showVault = false }
    if (showHistory) {
        AuraHistoryDialog(
            context = context,
            posture = emergencyPosture,
            appScan = emergencyScan,
            links = emptyList(),
            onUpdated = { historyEntries = it },
            onDismiss = { showHistory = false }
        )
    }
    if (showEmergency) {
        EmergencyModeDialog(
            steps = emergencySteps,
            currentStep = emergencyStep,
            running = emergencyRunning,
            result = emergencyResult,
            onAppDetails = { app -> openAppSettings(app.packageName) },
            onAppPermissions = { app -> openAppSettings(app.packageName) },
            onUninstall = { app -> requestUninstall(app.packageName) },
            onDismiss = { if (!emergencyRunning) showEmergency = false }
        )
    }
    moduleDialog?.let { (title, message) -> ModuleDialog(title, message) { moduleDialog = null } }
}

private fun linkAnalysisText(analysis: LinkAnalysis): String = buildString {
    appendLine("URL: ${analysis.url}")
    appendLine("Riesgo: ${analysis.risk}")
    append(analysis.reasons.joinToString("\n"))
}
